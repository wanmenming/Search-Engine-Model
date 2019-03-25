import math
import operator
from bs4 import BeautifulSoup
import os
import re

# tf-idf baseline
#documents number
N = 3204

# dictionary for term's document frequency
# term => doc_freq
doc_freq_dict = {}

# dictionary for term's document id
# term => doc_id
term_docId = {}

# dictionary for inverted index
# term => { docId => tf}
term_freq_dict = {}

# dictionary of cacm queries
# Num => query_string
queries_dict = {}



# Creates the dictionary with query Id and queries
def create_query_dict(location):
    data = open(location, 'r')
    soup = BeautifulSoup(data, 'lxml')  # parse xml
    docs = soup.find_all('doc')

    for doc in docs:
        doc_no = doc.find('docno')  # extract query id
        key = doc_no.text.strip()  # remove extra spaces and blank lines

        if len(doc_no) > 0:
            doc.find('docno').decompose()  # remove tag docno

        value = doc.text.strip()  # extract query text and remove extra spaces and blank lines
        queries_dict[key] = value


# Create the dictionary for the inverted index
def create_inverted_index_dict(location):
    count = 1
    data = open(location, 'r')

    for line in data:
        if count == 1:  # skip the header
            count += 1
            continue
        document_frequency_map = {}

        line_data = line.split(":")  # split each line in the file as (term, information)
        term = line_data.pop(0)

        tokens = line_data.pop(0).split(" ")  # split into => size of inverted list (document_id, frequency)
        doc_freq = tokens.pop(0)
        doc_freq_dict[term] = doc_freq

        for token in tokens:
            docid_freq = token.split(",")  # split into (document_id, frequency)
            doc_id = docid_freq.pop(0).lstrip('(')  # get doc ID
            freq = docid_freq.pop(0).rstrip(')\n')  # get term frequency for the doc ID
            document_frequency_map[doc_id] = freq

        term_freq_dict[term] = document_frequency_map

# To write the list to file
def write_to_file(scores, query_id, path):

    path = path + "TFIDF_Score_Query_" + query_id + ".txt"

    # if the file already exists, open it in append mode
    if os.path.exists(path):
        append_write = 'a'  # append if already exists
    else:  # else, create it and write to it
        append_write = 'w'  # make a new file if not

    count = 1
    output_file = open(path, append_write)
    for doc_id, score in scores:
        if count == 1:
            output_file.write("query_id Q0 doc_id rank Retrieval_Score system_name\n")
        if count > 100:
            break
        output_file.write("%s Q0 %s %s %s TFIDF_Model_CS6200\n" % (query_id, doc_id, count, score))
        count += 1

# Calculates tf-idf score for each query
def tf_idf_score(output_file):
    for query_id, query in queries_dict.items():
        query = query.strip()
        ' '.join(query.split())
        regex = r"(?<![a-z])[-.]?[\d]+([,.][\d]+)*|[a-zà-γ\d]+([-][a-zà-γ\d]+)*"   # process the query in the same was the corpus is processed
        query_terms = re.finditer(regex, query, re.UNICODE | re.IGNORECASE)

        query_count = dict()
        scores = dict()

        for query in query_terms:   # for each query term calculate query frequency
            query = query.group().lower()
            if query in query_count:
                query_count[query] = query_count[query] + 1
            else:
                query_count[query] = 1

        # term at a time
        for query, query_times in query_count.items():
            # term_freq_dict: term => { docId => tf}
            if query in term_freq_dict:  # check if the query term exists in the document
                doc_freq = int(doc_freq_dict[query])  # get document frequency of the term
                inverted_document_frequency = math.log(N/doc_freq)

                for doc_id, term_freq in term_freq_dict[query].items():
                    if doc_id in scores:
                        scores[doc_id] += int(term_freq) * inverted_document_frequency * query_times  # calculate tf-idf for document
                    else:
                        scores[doc_id] = int(term_freq) * inverted_document_frequency * query_times  # calculate tf-idf for document

        # sort scores in descending order
        scores_sorted = sorted(scores.items(), key=operator.itemgetter(1), reverse=True)

        # print top 100 scores to the file
        write_to_file(scores_sorted, query_id, output_file)


# Start tf-idf query calculation
def main():


    # location for cacm.query.txt file
    cacm_queries_location = "D:\\master study\\courses plan\\2018 Fall\\cs6200 Information retrieval\\project\\test-collection\\test-collection\\cacm.query.txt"

    # location for unigrams inverted indexes with term frequency
    inverted_index_location = "D:\\master study\\courses plan\\2018 Fall\\cs6200 Information retrieval\\project\\Number_Tables_Removed\\inverted_lists\\inverted_list_term_freqency.txt"

    # outfile file location
    output_file = "C:\\Users\\limin\\PycharmProjects\\IR-Project\\phase1\\Task1\\outputTFIDF\\"

    create_query_dict(cacm_queries_location)
    create_inverted_index_dict(inverted_index_location)


    tf_idf_score(output_file)


if __name__ == '__main__':
    main()