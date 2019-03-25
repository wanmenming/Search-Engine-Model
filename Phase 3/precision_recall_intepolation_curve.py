import pandas as pd
import matplotlib.pyplot as plt
import glob

def interpolation_curve_df(filepath):
    df_list = []


    all_files = glob.glob(filepath)

    for files in all_files:
        print(files)
        df_list.append(pd.read_csv(str(files), sep= ' '))

    return df_list, all_files

def interpolation_curve_plot(x):
    dfs, file_names = x
    print("the number of dataframes: " + str(len(dfs)))
    print("the number of files: " + str(len(file_names)))
    i =0
    fig = plt.figure(figsize = (12,22))
    while i < len(file_names):
        plt.plot(dfs[i]["recall"], dfs[i]["precision"], label = file_names[i].split('\\')[1][:-4], marker = 'o') # plot
        plt.xlabel("recall") # label the xaxis
        plt.ylabel("precision") # label y axis
        plt.grid("on") #make the grid
        #show plot
        i += 1
        plt.legend()
    plt.show()
    plt.savefig("line_plot.png") #save

def main():
    filepath = 'C:/Users/limin/PycharmProjects/IR-Project/phase3/outputplot/*txt'
    interpolation_curve_plot(interpolation_curve_df(filepath))


if __name__ == '__main__':
    main()
