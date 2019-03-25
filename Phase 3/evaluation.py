import os
import numpy as np
from collections import OrderedDict

# query_id => list of relevant documents
relevant_doc = {}

#rank => [ precision, recall, rel_lab]
rank_evaluation = {}

#top100 documents
rank_doc = []



# query = > {}
query_rank_eveluation = {}

#rank => [recall,average_precision]
interpolation_data ={}

# get the relevant dictionary
def create_rel_dict(location):
    data = open(location, 'r')
    for line in data:
        rel_doc = line.strip("\n").split(" ")
        query_id = rel_doc[0]
        doc_id = rel_doc[2]
        if query_id in relevant_doc:
            relevant_doc[query_id].append(doc_id)
        else:
            temp_doc_list = []
            temp_doc_list.append(doc_id)
            relevant_doc[query_id] = temp_doc_list
    #print(relevant_doc[34])


# To write the list to file
def write_to_interpolation_file(model,inter_precision, path):

    path = path + model + ".txt"

    # if the file already exists, open it in append mode
    if os.path.exists(path):
        append_write = 'a'  # append if already exists
    else:  # else, create it and write to it
        append_write = 'w'  # make a new file if not
    output_file = open(path, append_write)
    output_file.write("recall precision\n")
    for i in np.linspace(0,1,11):
        output_file.write("%s %s\n" % (i,inter_precision.pop(0)))

def write_to_table(docId,score,precision,recall,dir,model_name):
    for query_id,p_value in precision.items():
        path = dir + model_name +"_Precision_Recall_Table_For_Q" + query_id + ".txt"
        # if the file already exists, open it in append mode
        if os.path.exists(path):
            append_write = 'a'  # append if already exists
        else:  # else, create it and write to it
            append_write = 'w'  # make a new file if not
        output_file = open(path, append_write)

        output_file.write("queryId | docId | score | precision | recall\n")
        count = 0
        for p in p_value:
            output_file.write("%s | %s | %s | %s | %s\n" % (query_id,docId[query_id][count],score[query_id][count],p,recall[query_id][count]))
            count += 1


def write_to_p_k(docId,score,pk,dir,model_name,k):
    pk = {int(k): list(v) for k,v in pk.items()}
    pk_sorted = OrderedDict(sorted(pk.items()))
    pk_sorted = {str(k): list(v) for k,v in pk_sorted.items()}
    print(pk_sorted)
    if k == 5:
        path = dir + "Retrieval_System_P_@_5.txt"
        if os.path.exists(path):
            append_write = 'a'  # append if already exists
        else:  # else, create it and write to it
            append_write = 'w'  # make a new file if not
        output_file = open(path, append_write)
        if append_write == 'w':

            output_file.write("Retrieval_System | queryId | docId | score | p@5\n")

        for query_id,pk_value in pk_sorted.items():
            output_file.write("%s | %s | %s | %s | %s\n" % (model_name,query_id,docId[query_id][4],score[query_id][4],pk_value[0][0]))

    if k == 20:
        path = dir + "Retrieval_System_P_@_20.txt"
        if os.path.exists(path):
            append_write = 'a'  # append if already exists
        else:  # else, create it and write to it
            append_write = 'w'  # make a new file if not
        output_file = open(path, append_write)
        if append_write == 'w':

            output_file.write("Retrieval_System | queryId | docId | score | p@20\n")
        for query_id,pk_value in pk_sorted.items():
            output_file.write("%s | %s | %s | %s | %s\n" % (model_name,query_id,docId[query_id][19],score[query_id][19],pk_value[0][1]))



def write_map_mrr_to_file(model_name, path,map,mrr,p_k_5,p_k_20):
    path = path + "Retrieval_System_Evaluation.txt"
    if os.path.exists(path):
        append_write = 'a'  # append if already exists
    else:  # else, create it and write to it
        append_write = 'w'  # make a new file if not
    output_file = open(path, append_write)
    if append_write == 'w':
        output_file.write("Retrieval_System | MAP | MRR | p@5(avg)| p@20(avg)\n")
    output_file.write("%s | %s | %s | %s | %s\n" % (model_name,map,mrr,p_k_5,p_k_20))

# evaluate each model by different file location
def evaluate_effectiveness(model_name,location,evaluation_loc,plot_loc,table_loc,pk_loc):
    files = os.listdir(location)
    rr = 0.0

    #query_id => []
    P_K = {}
    Average_Precision = []
    Reciprocal_Rank = []
    #query_id => []
    precision = {}
    #query_id => []
    recall = {}

    #query_id=>[docId list]
    queryId_docId = {}
    #query_id=>[score list]
    queryId_score = {}

    # evaluate each query
    for file in files:
        data = open(location+"\\"+file, 'r')
        #initial the number of retrieval relevant document
        retrieval_relevant = 0
        #rel_lab = 0
        query_id = ''
        pk_list = []
        curr_presicion = []
        cuur_recall = []
        #rank_evaluation = {}
        # caculate each rank
        ap = 0.0
        first_rel = 0
        if (model_name == "jm_smoothed_baseline" or model_name == "jm_smoothed_query_refinement"):
            ignore_first_line = 0
        else:
            ignore_first_line = 1
        line_count = 1
        #calculate each query
        for line in data:
            if (line_count == 1 and ignore_first_line == 1):
                line_count += 1
                continue
            rel_lab = 0
            #evaluate_list = []
            rank_info = line.split()
            doc_id = rank_info[2]
            score = rank_info[4]
            retrieval_doc_number = int(rank_info[3])
            rank = int(rank_info[3])
            query_id = rank_info[0]


            #docId_score[query_id].append(score)
            if query_id in relevant_doc:
                if line_count == 2:
                    print(relevant_doc[query_id])
                    line_count += 1

                if query_id in queryId_docId:
                    queryId_docId[query_id].append(doc_id)
                else:
                    queryId_docId[query_id] = []
                    queryId_docId[query_id].append(doc_id)

                if query_id in queryId_score:
                    queryId_score[query_id].append(score)
                else:
                    queryId_score[query_id] = []
                    queryId_score[query_id].append(score)
                relevant_doc_list = relevant_doc[query_id]
                relevant_doc_num = len(relevant_doc_list)

                if doc_id in relevant_doc[query_id]:
                    rel_lab = 1
                    retrieval_relevant += 1
                    ap += retrieval_relevant / retrieval_doc_number
                    #print("find the relavent")
                    if first_rel == 0:
                        first_rel = 1
                        rr = 1 / rank
                p = retrieval_relevant / retrieval_doc_number
                r = retrieval_relevant / relevant_doc_num
                curr_presicion.append(p)
                cuur_recall.append(r)

        print(model_name,query_id)

        if query_id in relevant_doc:
            print("find the relavent")
            print(retrieval_relevant)
            if retrieval_relevant != 0:
                Average_Precision.append(ap / retrieval_relevant)
            precision[query_id] = curr_presicion
            recall[query_id] = cuur_recall
            #write_to_table(queryId_docId,queryId_score,precision,recall,table_loc,model_name)

            Reciprocal_Rank.append(rr)
            pk_list.append(curr_presicion[4])
            pk_list.append(curr_presicion[19])
            if query_id in P_K:
                P_K[query_id].append(pk_list)
            else:
                P_K[query_id] = []
                P_K[query_id].append(pk_list)
    write_to_table(queryId_docId,queryId_score,precision,recall,table_loc,model_name)
    write_to_p_k(queryId_docId,queryId_score,P_K,pk_loc,model_name,5)
    write_to_p_k(queryId_docId,queryId_score,P_K,pk_loc,model_name,20)
    sum_pk_5 = 0
    sum_pk_20 = 0
    for query_id,pk_value in P_K.items():
        for p_sublist in pk_value:
            sum_pk_5 += p_sublist[0]
            sum_pk_20 += p_sublist[1]
    average_pk_5 = sum_pk_5 / float(len(P_K))
    average_pk_20 = sum_pk_20 / float(len(P_K))
    map = sum(Average_Precision)/float(len(Average_Precision))
    mrr = sum(Reciprocal_Rank)/float(len(Reciprocal_Rank))
    write_map_mrr_to_file(model_name,evaluation_loc,map,mrr,average_pk_5,average_pk_20)

    interpolate_precision = []
    new_precision = []

    #print(interpolate_recall)

    for interpolate_recall in np.linspace(0,1,11):

        for query_id,r_value in recall.items():
            index = 0
            for recall_value in r_value:
                if interpolate_recall <= recall_value:
                    interpolate_precision.append(precision[query_id][index])
                    break
                index += 1
        average_precise = sum(interpolate_precision)/float(len(interpolate_precision))
        new_precision.append(average_precise)
    print(new_precision)
    write_to_interpolation_file(model_name,new_precision,plot_loc)


def get_immediate_subdirectories(a_dir):
    return [name for name in os.listdir(a_dir)
            if os.path.isdir(os.path.join(a_dir, name))]


# Start evaluations:
def main():
    # location for cacm.query.txt file
    cacm_rel_location = "D:\\master study\\courses plan\\2018 Fall\\cs6200 Information retrieval\\project\\test-collection\\test-collection\\cacm.rel.txt"

    #location for Top100 of all models' result, each model's files should be a sub folder. The name of sub-folder should be model name
    input_location = "C:\\Users\\limin\\PycharmProjects\\IR-Project\\phase3\\input"

    # outfile file location
    output_table = "C:\\Users\\limin\\PycharmProjects\\IR-Project\\phase3\\outputtable\\"
    output_file_plot = "C:\\Users\\limin\\PycharmProjects\\IR-Project\\phase3\\\outputplot\\"
    output_evaluation = "C:\\Users\\limin\\PycharmProjects\\IR-Project\\phase3\\evaluation\\"
    out_put_pk = "C:\\Users\\limin\\PycharmProjects\\IR-Project\\phase3\\outputpk\\"
    create_rel_dict(cacm_rel_location)
    sub_dir = get_immediate_subdirectories(input_location)
    for dir in sub_dir:
        model_name = dir
        model_result_location = "C:\\Users\\limin\\PycharmProjects\\IR-Project\\phase3\\input\\" + dir
        print(model_result_location)
        evaluate_effectiveness(model_name,model_result_location,output_evaluation,output_file_plot,output_table,out_put_pk)
    #write_to_table(precision,recall,output_table)




if __name__ == '__main__':
    main()