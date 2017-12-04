#!/usr/bin/perl

use Getopt::Long;
#use warnings;

GetOptions ("agent=s" => \$our_agent,    
	    "agent_path=s"   => \$agent_path,      
	    "num_of_tournaments=s"  => \$number_of_tournaments,
	    "nego_per_tourn=s" => \$nego_per_tournament,
	    "help" => \$help)   
    or die("Error in command line arguments\n");

#displaying help for now here.. TODO: Add a sub to do this ...
if($help){
    print ("\n Benchmarking tool help\n
      OPTIONS: \n
      -help : Displays this message 
      -agent : Name of the agent to benchmark. 
      -agent_path : Path of the .class file of the agent.
      -nego_per_tourn : Number of negotiations per tournament. Change this on the base XML file under Genius.
      Default value is 12, meaning two repetitions of 6 negotiaions. 
      -num_of_tournaments : Number of tournaments to benchmark. Default value is 4.
      Example of command: 
      RUN THIS SCRIPT on ROOT/scripts, be sure to have agents.dat here and a base XML under ROOT/genius/multiparty_custom_tournament.xml \n
      perl benchmark.pl -agent MrBeanFusion -agent_path /home/sleebarr/EMECS_SOTON/Intelligent_Agents/project/intelligent_agents/src/MrBeanFusion/MrBeanFusion.class -num_of_tournaments 10 -nego_per_tourn 12\n
      ");
    die;
}


my $num_of_agents;
my  $random_agent_1;
my  $random_agent_2;
#my $agent_path;
my $domain;
my $it;
#my $our_agent;

#This stuff is global ..
my $global_utility;
my $global_nash_distance;
my $global_pareto_distance;
my $global_wins;
my $global_fails;
#my $nego_per_tournament;
my @agents;
my $num_of_domains;

$num_of_domains = 8;

open (agent_database, "<" , "agents.dat")
    or die ("\n Can not open agents database agents.dat...\n"); 

@agents = <agent_database>;

$global_nash_distance = 0;
$global_pareto_distance = 0;
$global_utility = 0;
$global_wins = 0;
$global_fails = 0;

$num_of_agents = 16;
$domain = 0;
$it = 0;

#Defaults ...
if(!$our_agent){
    $our_agent = "MrBeanFusion";
}

if(!$nego_per_tournament){
    $nego_per_tournament = 12;    
}

if(!$number_of_tournaments){
    $number_of_tournaments = 2;
}

if(!$agent_path){
    $agent_path = "/home/sleebarr/EMECS_SOTON/Intelligent_Agents/project/intelligent_agents/src/MrBeanFusion/MrBeanFusion.class";
}

#File handles
my $CUSTOM_TOURNAMENT;
my $OUTFILE;
my $LOG;

print ("\nI: Benchmarking tool starting ...\n");
print ("I: Benchmark agent : $our_agent ...\n");
print ("I: Agent path : $agent_path ...\n");
print ("I: Number of tournaments : $number_of_tournaments ...\n");
print ("I: Negotiations per tournament : $nego_per_tournament ...\n");
open ( $CUSTOM_TOURNAMENT , "<" , "multiparty_custom_tournament.xml");
for(my $it = 0 ; $it < $number_of_tournaments ; $it++){
    $random_agent_1 = $agents [int (rand($num_of_agents-1)) + 1];
    $random_agent_2 =  $agents [int (rand($num_of_agents-1)) + 1];
    open ( $CUSTOM_TOURNAMENT , "<" , "multiparty_custom_tournament.xml");
    print "I: Opening file multiparty_custom_tournament_gen_$it.xml\n";
    open ( $OUTFILE ,  '>', "multiparty_custom_tournament_gen_$it.xml"  ) or die;
    @lines = <$CUSTOM_TOURNAMENT>;
    $size = @lines;
    chomp($random_agent_1);
    chomp($random_agent_2);
    print ("I: Generating random configuration ...;\n");
    print ("I: Random agent 1 : $random_agent_1\n");
    print ("I: Random agent 2 : $random_agent_2\n");

    $domain = int (rand($num_of_domains));

    for( $i = 0  ; $i < $size ; $i++){
	if($lines[$i] =~ /<partyRepItems/){
	    print  $OUTFILE $lines[$i];
	    $i++;
	    print $OUTFILE "\t\t<party classPath=".'"'.$agent_path.'">';
	    $i++;
	    print $OUTFILE $lines[$i];
	    $i++;
	    print $OUTFILE $lines[$i];
	    $i++;
	    print $OUTFILE "\t\t<party classPath=".'"'.$random_agent_1.'">';
	    $i++;
	    print $OUTFILE $lines[$i];
	    $i++;
	    print $OUTFILE $lines[$i];	
	    $i++;
	    print $OUTFILE "\t\t<party classPath=".'"'.$random_agent_2.'">';	
	}
	elsif ($lines[$i] =~ /<partyProfileItems/){
	    if($domain == 0) {
		print ("I: Running with party domain ...\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/partydomain/party1_utility.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/partydomain/party2_utility.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/partydomain/party3_utility.xml" />'."\n" ;	
	    } 	
	    elsif($domain == 1) {
		print ("I: Running with university domain  ...\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group1-university/University_util1.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group1-university/University_util2.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group1-university/University_util3.xml" />'."\n" ;	
	    }
	    elsif($domain == 2) {
		print ("I: Running with  politics domain ...\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group2-politics/Politics_util1.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group2-politics/Politics_util2.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group2-politics/Politics_util3.xml" />'."\n" ;	
	    }
	    elsif($domain == 3) {
		print ("I: Running with japan trip... WARNING: Modify this domain to make the reservation value and discount 0!\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/SYAgent/JapanTrip_util1.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/SYAgent/JapanTrip_util2.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/SYAgent/JapanTrip_util3.xml" />'."\n" ;	
	    }
	    elsif($domain == 4) {
		print ("I: Running with bank robbery domain WARNING: Modify this domain to make the reservation value and discount 0! ...\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group3-bank_robbery/Bank_Robbery_util1.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group3-bank_robbery/Bank_Robbery_util2.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group3-bank_robbery/Bank_Robbery_util3.xml" />'."\n" ;	
	    }
	    elsif($domain == 5) {
		print ("I: Running with smart domain inner ...\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/Caduceus/Smart_Grid_util1.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/Caduceus/Smart_Grid_util2.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/Caduceus/Smart_Grid_util3.xml" />'."\n" ;	
	    }
	    elsif($domain == 6) {
		print ("I: Running with Penergy domain inner ...\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/parsAgent2/PEnergy_util1.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/parsAgent2/PEnergy_util2.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2016/parsAgent2/PEnergy_util3.xml" />'."\n" ;	
	    }	 
	    elsif($domain == 7) {
		print ("I: Running with new sporthal domain inner ...\n");
		print  $OUTFILE $lines[$i];
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group2-new_sporthal/New_sporthal_util1.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group2-new_sporthal/New_sporthal_util2.xml" />'."\n" ;
		$i++;
		print $OUTFILE '<item url="file:etc/templates/ANAC2015/group2-new_sporthal/New_sporthal_util1.xml" />'."\n" ;	
	    }	
	    
	    
	    
	    
	}
	else{
	    print $OUTFILE  ($lines[$i]);
	}    
    }

    #close $OUTFILE or die;
    #close $CUSTOM_TOURNAMENT or die;

    print ("I: Initializing tournament ....\n");

    chdir "../genius";
    
    my $cmd = ("java -cp negosimulator.jar negotiator.xml.multipartyrunner.Runner ../script/multiparty_custom_tournament_gen_$it.xml log/tournament_$it.xml");

    print ("I: Running genius with command : $cmd ....\n");
    
    system("java -cp negosimulator.jar negotiator.xml.multipartyrunner.Runner ../script/multiparty_custom_tournament_gen_$it.xml log/tournament_$it.xml");

    print ("I: Tournament done parsing information ....\n");
    print ("I: Opening file log/tournament_$it.xml.csv ....\n");
    open ($LOG , "<" , "log/tournament_$it.xml.csv") or die;

    @lines = <$LOG>;

    $lines = @lines;

    my @fields;

    my ($agent_1 , $agent_2 , $agent_3);
    my ($util_1 , $util_2 , $util_3);
    my $failed_negotiations;
    my ($our_agent_number , $our_agent_utility);
    my ($avg_utility , $nash_distance ,  $avg_nash , $avg_pareto , $pareto_distance);
    my $number_of_wins;
    $failed_negotiations = 0;
    
    $avg_pareto = 0;
    $avg_nash = 0;
    $avg_utility = 0;
    $number_of_wins = 0;

    my $curr_util, $max_util;
    
    for ($i = 2 ; $i < $lines ; $i++){
	$max_util = 0;   
	chomp($lines[$i]); 
	@fields = split ";" , $lines[$i];

	#parse agents
	$agent_1 = $fields[12];
	$agent_2 = $fields[13];
	$agent_3 = $fields[14]; 

	#parse utility
	$util_1 = $fields[15];
	$util_2 = $fields[16];
	$util_3 = $fields[17];

	$pareto_distance = $fields [9];
	$nash_distance = $fields [10];
	
	if($agent_1 =~ $our_agent){
	    $our_agent_utility = $util_1;
	}
	elsif($agent_2 =~ $our_agent){
	    $our_agent_utility = $util_2;
	}
	elsif($agent_3 =~ $our_agent){
	    $our_agent_utility = $util_3;
	} 

        $max_util = $fields[8];
	
	print ("@ $i $our_agent utility: $our_agent_utility max utility : $max_util \n ");

	if(($our_agent_utility == $max_util) && $max_util != 0){
	    $number_of_wins ++;
	}

	if($max_util == 0) {
	    $failed_negotiations++;
	}

	$avg_pareto += $pareto_distance;
	$avg_nash += $nash_distance;
	$avg_utility += $our_agent_utility;
	
    }

    $number_of_wins = ($number_of_wins * 100)/ $nego_per_tournament;
    $avg_utility = $avg_utility / $nego_per_tournament;
    $avg_pareto = $avg_pareto / $nego_per_tournament;
    $avg_nash = $avg_nash / $nego_per_tournament;
    $failed_negotiations = ($failed_negotiations * 100 ) /  $nego_per_tournament;
    
    print ("\nTournament $it summary : \n 
Agent : $our_agent\n
Average utility : $avg_utility\n
Average pareto  : $avg_pareto\n
Average nash    : $avg_nash \n
Percentage Wins : $number_of_wins %\n
Failed negotiations : $failed_negotiations \n
");

    $global_utility += $avg_utility ;
    $global_nash_distance += $avg_nash;
    $global_pareto_distance += $avg_pareto;
    $global_wins += $number_of_wins;
    $global_fails += $failed_negotiations;
        chdir "../script";
}

$global_utility  = $global_utility / $number_of_tournaments;
$global_nash_distance = $global_nash_distance  / $number_of_tournaments;
$global_pareto_distance = $global_pareto_distance /$number_of_tournaments;
$global_wins = $global_wins /$number_of_tournaments;
$global_fails = $global_fails / $number_of_tournaments;

print ("\n SUMMARY \n 
           Number of tournaments : $number_of_tournaments\n
           Global utility : $global_utility \n
           Nash : $global_nash_distance \n
           Pareto : $global_pareto_distance \n
           Wins : $global_wins % \n 
           Failed Negotiations : $global_fails % \n");

close $OUTFILE;
close $CUSTOM_TOURNAMENT;
__END__
