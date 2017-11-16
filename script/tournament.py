import subprocess
import os
import csv
import matplotlib.pyplot as plt
from matplotlib import gridspec
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
number_agreements = 0
number_negotiations = 0


def update_config(deadline_value, deadline_type, repeats):
    """
    Function used to update the tournament configuration file
    :param deadline_value: value of the deadline, should be of type int
    :param deadline_type: type of the deadline, either ROUND or TIME
    :param repeats: number of times that the tournament should be repeated
    """
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
            global number_agreements, number_negotiations
            number_agreements = number_agreements + (1 if row['Agreement'] == "Yes" else 0)
            number_negotiations = number_negotiations + 1
            if not agents_utilities:
                agents_utilities[row['Agent 1'][:row['Agent 1'].find('@')]] = []
                agents_utilities[row['Agent 2'][:row['Agent 2'].find('@')]] = []
                agents_utilities[row['Agent 3'][:row['Agent 3'].find('@')]] = []
            agents_utilities[row['Agent 1'][:row['Agent 1'].find('@')]].append(float("{0:.3f}".format(float(row['Utility 1']))))
            agents_utilities[row['Agent 2'][:row['Agent 2'].find('@')]].append(float("{0:.3f}".format(float(row['Utility 2']))))
            agents_utilities[row['Agent 3'][:row['Agent 3'].find('@')]].append(float("{0:.3f}".format(float(row['Utility 3']))))


def show_results():
    """
    Function used to parse the results and plot them
    """
    keys = agents_utilities.keys()
    values = agents_utilities.values()
    n_wins = [0, 0, 0]
    value_per_round = map(list, zip(*values))
    for utilities in value_per_round:
        maximum = max(utilities)
        if maximum != 0:
            n_wins[utilities.index(maximum)] += 1
    print("Agent " + keys[0] + " ranked 1st: " + str(n_wins[0]) + " times in " + str(number_negotiations) + " negotiations")
    print("Agent " + keys[1] + " ranked 1st: " + str(n_wins[1]) + " times in " + str(number_negotiations) + " negotiations")
    print("Agent " + keys[2] + " ranked 1st: " + str(n_wins[2]) + " times in " + str(number_negotiations) + " negotiations")
    print("Number of successful negotiations is: " + str(number_agreements))
    print("Number of failed negotiations is: " + str(number_negotiations - number_agreements))

    fig = plt.figure()
    gs = gridspec.GridSpec(2, 2)

    bar_width = 0.2
    opacity = 0.7

    ax1 = fig.add_subplot(gs[0, :])
    index = np.arange(len(values[0]))
    ax1.bar(index + 0*bar_width, values[0], bar_width, alpha=opacity, color='b', label=keys[0])
    ax1.bar(index + 1*bar_width, values[1], bar_width, alpha=opacity, color='r', label=keys[1])
    ax1.bar(index + 2*bar_width, values[2], bar_width, alpha=opacity, color='g', label=keys[2])
    ax1.set_ylabel('Utilities')
    ax1.set_title('Utility Comparison')
    ax1.set_ylim([0, 1])

    bar_width = 0.5
    ax2 = fig.add_subplot(gs[1, 0])
    index = np.arange(len(dist_to_pareto))
    ax2.bar(index, dist_to_pareto, bar_width, alpha=opacity, label=index+1)
    ax2.set_title('Distance to Pareto Frontier')
    ax2.set_ylim([0, 2])

    ax3 = fig.add_subplot(gs[1, 1])
    ax3.bar(index, dist_to_nash, bar_width, alpha=opacity, label=index + 1)
    ax3.set_title('Distance to Nash Product')
    ax3.set_ylim([0, 2])

    plt.tight_layout()
    plt.show()


if __name__ == "__main__":
    update_config(200, "ROUND", 1)
    run_tournament(tournament_config, output_log)
    parse_results(output_log)
    show_results()
