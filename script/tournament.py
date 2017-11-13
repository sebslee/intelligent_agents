import subprocess
import os
import csv
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
import numpy as np
try:
    import xml.etree.cElementTree as ET
except ImportError:
    import xml.etree.ElementTree as ET

working_directory = '../genius/'
command = 'java -cp negosimulator.jar negotiator.xml.multipartyrunner.Runner'
tournament_config = 'multiparty_custom_tournament.xml'
output_log = '../logs/tournament_log'

max_utilities = []
min_utilities = []
dist_to_pareto = []
dist_to_nash = []
agents_utilities = {}

def update_config(deadline_value, deadline_type, repeats):
    tree = ET.ElementTree(file=os.path.dirname(os.getcwd())+"/genius/"+tournament_config)
    for elem in tree.iter():
        if elem.tag == "deadline":
            elem[0].text = str(deadline_value)
            elem[1].text = str(deadline_type)
        elif elem.tag == "partyRepItems":
            pass
        elif elem.tag == "partyProfileItems":
            pass
        elif elem.tag == "repeats":
            elem.text = str(repeats)
    tree.write(os.path.dirname(os.getcwd())+"/genius/"+tournament_config, "UTF-8")

def run_tournament(cfg, out):
    """
    Function that runs a multilateral tournament with parameters in cfg and outputs a log in out
    :param cfg: Configuration file for the tournament
    :param out: Log file of the tournament
    """
    subprocess.call(" ".join([command,cfg,out]), shell=True, cwd=working_directory)


def parse_results(log):
    """
    Function that parses the result obtained from running the negotiation tournament
    :param log: path to the log file
    """
    log_file = log + '.csv'
    with open(log_file, 'rb') as csv_file:
        separator = csv_file.readline()[4]
        log_reader = csv.DictReader(csv_file, delimiter=separator)
        for row in log_reader:
            max_utilities.append(row['max.util.'])
            min_utilities.append(row['min.util.'])
            dist_to_pareto.append(row['Dist. to Pareto'])
            dist_to_nash.append(row['Dist. to Nash'])
            if not agents_utilities:
                agents_utilities[row['Agent 1'][:row['Agent 1'].find('@')]] = []
                agents_utilities[row['Agent 2'][:row['Agent 2'].find('@')]] = []
                agents_utilities[row['Agent 3'][:row['Agent 3'].find('@')]] = []
            agents_utilities[row['Agent 1'][:row['Agent 1'].find('@')]].append(float("{0:.3f}".format(float(row['Utility 1']))))
            agents_utilities[row['Agent 2'][:row['Agent 2'].find('@')]].append(float("{0:.3f}".format(float(row['Utility 2']))))
            agents_utilities[row['Agent 3'][:row['Agent 3'].find('@')]].append(float("{0:.3f}".format(float(row['Utility 3']))))


def show_results():
    keys = agents_utilities.keys()
    values = agents_utilities.values()
    n_wins = [0, 0, 0]
    for utilities in map(list, zip(*values)):
        n_wins[utilities.index(max(utilities))] += 1
    print("Agent " + keys[0] + " ranked 1st: " + str(n_wins[0]) + " times")
    print("Agent " + keys[1] + " ranked 1st: " + str(n_wins[1]) + " times")
    print("Agent " + keys[2] + " ranked 1st: " + str(n_wins[2]) + " times")
    plt.subplots()
    index = np.arange(len(values[0]))
    bar_width = 0.2
    opacity = 0.7
    rects1 = plt.bar(index + 0*bar_width, values[0], bar_width, alpha=opacity, color='b', label=keys[0])
    rects2 = plt.bar(index + 1*bar_width, values[1], bar_width, alpha=opacity, color='r', label=keys[1])
    rects3 = plt.bar(index + 2*bar_width, values[2], bar_width, alpha=opacity, color='g', label=keys[2])
    plt.ylabel('Utilities')
    plt.title('Utility Comparison')
    plt.xticks(index+1.5*bar_width, index+1)
    plt.legend()
    plt.tight_layout()
    fig = plt.figure()
    ax = Axes3D(fig)
    ax.plot(values[0], values[1], values[2], 'ro')
    ax.set_xlabel(keys[0])
    ax.set_ylabel(keys[1])
    ax.set_zlabel(keys[2])
    """
    x, y = np.meshgrid(np.linspace(0, 1, 100), np.linspace(0, 1, 100))
    z = (0.5*x + 0.5*y)
    ax.plot_surface(x, y, z, rcount = 10, ccount = 10, alpha=1.0)
    """
    plt.show()


if __name__ == "__main__":
    update_config(200, "ROUND", 3)
    run_tournament(tournament_config, output_log)
    parse_results(output_log)
    show_results()