import subprocess

working_directory = '../genius/'
command = 'java -cp negosimulator.jar negotiator.xml.multipartyrunner.Runner '
tournament_config = 'multiparty_custom_tournament.xml '
output_log = '../logs/tournament_log'

if __name__ == "__main__":
    subprocess.call(command+tournament_config+output_log, shell=True, cwd=working_directory)
