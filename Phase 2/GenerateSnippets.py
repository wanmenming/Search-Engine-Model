from bs4 import BeautifulSoup
import re
import os
from nltk.util import ngrams
import nltk

# dictionary of cacm queries
queries_dict = {}

# dictionary for corpus term frequencies
corpus_dict = {}

# dictionary for JM Smoothed Query Likelihood Scores
jm_scores_dict = {}

# Maximum length of the generated snippet
MAX_SNIPPET_SIZE = 100


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


# Creates the dictionary for term and its frequency in the corpus
def create_corpus_tf(location):
    data = open(location, 'r')
    count = 1

    for line in data:
        if count == 1:   # skip the heading
            count += 1
            continue

        tf = line.strip("\n").split(" ")
        corpus_dict[tf[0]] = tf[1]


# Creates the dictionary for query id and its top 100 doc ids
def create_jm_score_dict(filename):
    file_list = os.listdir(filename)

    for file in file_list:  # for each query score file
        path = filename + file
        data = open(path, 'r')
        count = 1

        for line in data:
            if count == 1:  # skip the heading
                count += 1
                continue

            record_fields = line.split(" ")
            query_id = record_fields[0]
            doc_id = record_fields[2]
            # save the query id and the list of top 100 relevant documents for that query
            if query_id in jm_scores_dict:
                jm_scores_dict[query_id].append(doc_id)
            else:
                jm_scores_dict[query_id] = [doc_id]


# Checks if the n-gram exists in the snippet. If it does, generates the snippet
def is_ngram_present(corpus_location, query, doc, n):
    query = query.strip()
    ' '.join(query.split())
    regex = r"(?<![a-z])[-.]?[\d]+([,.][\d]+)*|[a-zà-γ]+([-][a-zà-γ]+)*"  # process query same as corpus
    matches = re.finditer(regex, query, re.UNICODE | re.IGNORECASE)
    query_terms = []
    for match in matches:
        query_terms.append(match.group().lower())

    doc_loc = corpus_location + doc + ".html"
    soup = BeautifulSoup(open(doc_loc), "html.parser")  # parse the document
    doc_content = soup.find('pre').get_text()  # get all the content in the <PRE> tag
    # convert the document text to lower case for matching with n-grams
    regex = r"[A-Z][\.]"
    cap_dot = len(re.findall(regex, doc_content))

    regex = r"[\.]"
    dot = len(re.findall(regex, doc_content))

    sentence_count = dot - cap_dot

    doc_content_lc = soup.find('pre').get_text().lower()
    doc_len = len(doc_content)  # get content length

    generated_ngrams = ngrams(query_terms, n)  # generate n-grams

    expected_freq = 0

    if sentence_count < 25:
        expected_freq = 7 - 0.1 * (25 - sentence_count)
    elif sentence_count >= 25 and sentence_count <= 40:
        expected_freq = 7
    else:
        expected_freq = 7 - 0.1 * (sentence_count - 40)

    ngrams_map = nltk.FreqDist(generated_ngrams)

    ngrams_list = []

    selected_ngram = ''

    for key, value in ngrams_map.items():
        if value >= expected_freq:
            selected_ngram = ' '.join(str(k) for k in key)
        else:
            ngrams_list.append(' '.join(str(k) for k in key))

    if selected_ngram != '' and doc_content_lc.find(selected_ngram) != -1:
        return create_snippet(doc_content_lc, selected_ngram, n, doc_len, doc_content)
    else:
        for gram in ngrams_list:  # for each n-gram term
            if doc_content_lc.find(gram) != -1:  # check if it exists in the document
                return create_snippet(doc_content_lc, gram, n, doc_len, doc_content)

    return ""


# create snippet for the term
def create_snippet(doc_content_lc, term, n, doc_len, doc_content):
    gram_index = doc_content_lc.index(term)  # get n-gram position in the corpus
    start_index = max(gram_index - MAX_SNIPPET_SIZE, 0)
    end_index = min(gram_index + n + MAX_SNIPPET_SIZE, doc_len)

    # if the start index of the snippet is in the middle of a word, get the entire word
    while start_index > 0 and doc_content_lc[start_index] != " " and doc_content_lc[start_index] != "\n":
        start_index -= 1

    # if the end index of the snippet is in the middle of a word, get the entire word
    while end_index < doc_len and doc_content_lc[end_index] != " " and doc_content_lc[end_index] != "\n":
        end_index += 1

    highlight_start_index = gram_index
    highlight_end_index = gram_index + len(term)

    # get the highlighted word from the corpus
    highlight_word = doc_content[highlight_start_index:highlight_end_index]

    # get the snippet text from the corpus and highlight the relevant word
    snippet = doc_content[start_index:end_index].replace(highlight_word, "<b>{}</b>".format(highlight_word))
    return snippet


# Start with tri gram query from text and keep looking for snippet terms.
#  If no snippet found, try bi-grams and uni-grams
def generate_snippet_text(corpus_path, query, doc):
    n = 3
    while n > 0:
        snippet = is_ngram_present(corpus_path, query, doc, n)
        if snippet != "":
            return snippet
        n -= 1
    return ""


# Print snippets to the file
def write_snippet(output_path, corpus_path):

    for query_id, query in queries_dict.items():
        path = output_path + "snippet_Query_" + query_id + ".html"

        file = open(path, 'w')
        file.write("<!DOCTYPE html><i><u>Query ID: " + query_id + " </u></i><br /><br />")
        for doc in jm_scores_dict[query_id]:
            file.write("&nbsp&nbsp&nbsp&nbsp <i>Doc ID: " + doc + " </i><br /><br />")
            snippet = generate_snippet_text(corpus_path, query, doc)
            file.write("&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp... " + snippet + " ...<br /><br />")
        file.write("<br \>")
        file.close()


def main():
    # location of the corpus
    corpus_location = "D:\Manpreet\sem4\IR\project\\test-collection\cacm\\"

    # location for cacm.query.txt file
    cacm_queries_location = "D:\Manpreet\sem4\IR\project\\test-collection\cacm.query.txt"

    # location for corpus statistics file
    tf_location = "D:\Manpreet\sem4\IR\project\\Number_Tables_Removed\inverted_lists\corpus_statistics.txt"

    # location for JM Smoothed Query Likelihood scores
    jm_score_location = "D:\Manpreet\\repo\IR-Project\phase1\Task1\output\\"

    # output file path
    output_path = "D:\Manpreet\\repo\IR-Project\phase2\output\\"

    create_query_dict(cacm_queries_location)
    create_corpus_tf(tf_location)
    create_jm_score_dict(jm_score_location)

    write_snippet(output_path, corpus_location)


if __name__ == '__main__':
    main()
