package mrBean_2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
import negotiator.BidHistory;
import negotiator.boaframework.OutcomeSpace;
import negotiator.issue.*;
import negotiator.utility.*;

import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

public class mrBean extends AbstractNegotiationParty {

	private final String description = "MrBean 2";

	private class OpponentModel {
		private double Issues [][];
		private double Weights [];
		public int frequency [][];
		private int N_issues;
		private int N_values;
		private int maximum_freqs [];
		private int weight_ranking[];
		private int value_ranking[];

		public OpponentModel(int num_issues, int num_values, int [][] freq, List<Issue> domain_issues) {
			Issues = new double [num_issues][num_values];
			Weights = new double [num_issues];
			frequency = freq;
			N_issues = num_issues;
			N_values = num_values;
			maximum_freqs = new int[N_issues];
			weight_ranking = new int [N_issues];
			value_ranking = new int [N_values];
		}

		public void updateModel() {
			System.out.println("");
			int j = 0;
			int k = 0;
			int i = 0;
			for(i = 0; i < N_issues; i++) {
				maximum_freqs[i] = Collections.max(Arrays.asList(ArrayUtils.toObject(frequency[i])));
				if(maximum_freqs[i] == 0) {
					maximum_freqs[i] = 1;
				}
				weight_ranking[i] = N_issues-i;
				j = i - 1;
				while(j != -1) {
					if(maximum_freqs[i] > maximum_freqs[j]) {
						weight_ranking[i] = Math.max(weight_ranking[j], weight_ranking[i]);
						weight_ranking[j]--;
					}
					j--;
				}
				//System.out.format("\nIndex %d, Maximum Frequency = %d\n", i, maximum_freqs[i]);
				for(j = 0; j < N_values; j++) {
					value_ranking[j] = N_values-j;
					k = j - 1;
					while(k != -1) {
						if(frequency[i][j] > frequency[i][k]) {
							value_ranking[j]++;
							value_ranking[k]--;
						}
						if(frequency[i][j] == frequency[i][k]) {
							value_ranking[k]--;
							value_ranking[j] = Math.min(value_ranking[k], value_ranking[j]);
						}
						k--;
					}
				}
				for(j = 0; j < N_values; j++) {
					Issues[i][j] = value_ranking[j]*1.0/N_values;
					System.out.format("\nFrequency %d = %d", j, frequency[i][j]);
					System.out.format("\nRanking %d = %d", j, value_ranking[j]);
					System.out.format("\nValue %d, Predicted Evaluation = %f\n", j, Issues[i][j]);
				}
			}
			for(i = 0; i < N_issues; i++) {
				//Weights[i] = maximum_freqs[i]/freq_sum;
				//System.out.format("\nRanking %d = %d", i, weight_ranking[i]);
				Weights[i] = 2.0*weight_ranking[i]/(N_issues*(N_issues+1.0));
				System.out.format("\nIndex %d, Normalized Weight = %f\n", i, Weights[i]);
			}
			System.out.println("*******************************\n");
		}

		public double predictUtility(Bid bid) {
			double U = 0.0;
			for(Issue issue: bid.getIssues()) {
				U = U + 
						Issues[issue.getNumber()-1][((IssueDiscrete) issue).getValueIndex((ValueDiscrete)(bid.getValue(issue.getNumber())))]*Weights[issue.getNumber()-1];
			}
			System.out.format("\nPredicted utility = %f\n", U);
			return U;
		}

		public int getIssueMaxValueIndex(int issue_index) {
			int max_index = 0;
			for(int i = 0; i < N_values; i++) {
				if(Issues[issue_index][max_index] < Issues[issue_index][i]) {
					max_index = i;
				}
			}
			return max_index;
		}

		public double getIssueValue(int issue_index, int value_index) {
			return Issues[issue_index][value_index];
		}

		public double getWeight(int issue_index) {
			return Weights[issue_index];
		}
	};

	private OpponentModel opponentA;
	private OpponentModel opponentB;

	private Bid lastReceivedOffer; // offer on the table
	private Bid myLastOffer;
	private Bid maxUtilityOffer;
	private Bid minUtilityOffer;
	private OutcomeSpace outcome_space;

	private double Umax;
	private double Umin;

	private double k;
	private double b;

	private double percent_increase;

	private int hashcode_a;
	private int hashcode_b;

	private BidHistory agentAhistory ;
	private BidHistory agentBhistory ;

	private java.util.List<Issue> domain_issues; 
	private java.util.List<ValueDiscrete> values;
	EvaluatorDiscrete evaluator;

	// Two dimensional array, number of issues times number of max number of values ...
	int freq_a [][];  
	int freq_b [][];
	double max_weight = 0;
	double min_weight = 1;
	AdditiveUtilitySpace additiveUtilitySpace_i;


	@Override
	public void init(NegotiationInfo info) {
		super.init(info);
		// Initialize the outcome_space variable and generate all bids
		//outcome_space = new OutcomeSpace(this.utilitySpace);
		//outcome_space.generateAllBids(this.utilitySpace);
		// Initialize the Agent's histories and the hashcodes to 0
		hashcode_a = 0;
		hashcode_b = 0;
		//agentAhistory= new BidHistory();
		//agentBhistory = new BidHistory();
		// Get the Max and Min Utility Offers
		maxUtilityOffer = this.getMaxUtilityBid();
		minUtilityOffer = this.getMinUtilityBid();
		// Initialize the parameters used to compute the target utility
		Umax = 1.0;
		Umin = (this.utilitySpace.getUtility(minUtilityOffer)+Umax)/2.0;
		k = 0.2;
		b = 1.5;
		percent_increase = 0.05;

		// Get the Domain Issues
		domain_issues = this.utilitySpace.getDomain().getIssues();
		System.out.format("Domain has %d issues\n ", domain_issues.size());
		double curr_weight;

		// Cast the Utility Space into an Additive Utility Space to have access to its methods
		additiveUtilitySpace_i = (AdditiveUtilitySpace)this.utilitySpace;

		int max_num_of_values;
		max_num_of_values = 0;

		for(Issue lIssue : domain_issues) {
			System.out.format("MR BEAN INIT: %s number %d \n", lIssue.getName() , lIssue.getNumber() );
			IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
			evaluator = (EvaluatorDiscrete) (((AdditiveUtilitySpace)this.utilitySpace).getEvaluator(lIssue.getNumber()));
			values = lIssueDiscrete.getValues();
			curr_weight = (additiveUtilitySpace_i.getWeight(lIssue.getNumber()));
			if(curr_weight < min_weight) {
				min_weight = curr_weight;
			}
			if(curr_weight > max_weight){
				max_weight = curr_weight;
			}	
			if(values.size() > max_num_of_values){
				max_num_of_values = values.size();

				//System.out.format("New maximum value of values %d \n", max_num_of_values);
			}
			for (ValueDiscrete value : values) {
				try{
					System.out.format("MR BEAN VALUE : %d %s %f \n" , lIssueDiscrete.getValueIndex(value.getValue()), value.getValue(), evaluator.getEvaluation(value));}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.out.format("Max issue weight %f \n", max_weight);
		//System.out.format("Building frequency array with %d issues and %d max values.. ", domain_issues.size() , max_num_of_values);
		freq_a = new int [domain_issues.size()][max_num_of_values];
		freq_b = new int [domain_issues.size()][max_num_of_values];

		// Initiliaze the Opponent Models
		opponentA = new OpponentModel(domain_issues.size(), max_num_of_values, freq_a, domain_issues);
		opponentB = new OpponentModel(domain_issues.size(), max_num_of_values, freq_b, domain_issues);

		//Lets initialize the array 
		for(int i = 0 ; i < domain_issues.size() ; i ++){
			for(int j = 0 ; j < max_num_of_values ; j++){
				freq_a [i][j] = 0;
				freq_b [i][j] = 0;
			}
		}

	}


	public Action chooseAction(List<Class<? extends Action>> list) {
		double panic;
		panic = 0.98;
		// According to Stacked Alternating Offers Protocol list includes
		// Accept, Offer and EndNegotiation actions only.
		if(lastReceivedOffer == null) {
			//Lets start with our maximum because we are bad boys
			System.out.println("\nOffering Maximum Utility Bid at the beginning");
			myLastOffer = maxUtilityOffer;                
			return new Offer(this.getPartyId(), myLastOffer);
		}

		//Every now and then just offer our maximum, depending on time..
		if(getTimeLine().getTime() < 0.4){
			//myLastOffer = this.getMaxUtilityBid();
			//System.out.println("\nOffering Maximum Utility Bid");
			//System.out.format("\nMaximum Utility is %f\n", this.utilitySpace.getUtility(maxUtilityOffer));
			return new Offer(this.getPartyId(), maxUtilityOffer);
		}

		// Compute the target utility
		double util;
		util = this.getTargetUtility(k, b);
		//System.out.format("\nMrBean: Target utility %f \n", util);

		System.out.println("Agent A");
		opponentA.updateModel();
		System.out.println("Agent B");
		opponentB.updateModel();

		if(acceptOrOffer(lastReceivedOffer, util)) {
			System.out.println("\nAccepting Offer");
			return new Accept(this.getPartyId(), lastReceivedOffer);
		}
		else if (getTimeLine().getTime() > panic){
			return new Accept(this.getPartyId(), lastReceivedOffer);
		}
		else {
			//return new Offer(this.getPartyId(), myLastOffer);
			//Return average bid then!
			//myLastOffer =  getAverageBid();
			myLastOffer = generateBid();
			System.out.format("\n\nMaking Offer with utility = %f", this.utilitySpace.getUtility(myLastOffer));
			opponentA.predictUtility(myLastOffer);
			opponentB.predictUtility(myLastOffer);
			return new Offer (this.getPartyId(), myLastOffer);    
		}
	}


	public void receiveMessage(AgentID sender, Action act) {

		super.receiveMessage(sender, act);
		//BidDetails lastReceivedOfferDetails;
		// System.out.format(" TIME %f\n",getTimeLine().getTime() );

		if (act instanceof Accept && lastReceivedOffer != null) {
			if(hashcode_a == 0) {
				// Agent A is the first agent to make an offer
				hashcode_a = sender.hashCode();
			}
			else if (hashcode_b == 0) {
				// Agent B is the second agent to make an offer
				hashcode_b = sender.hashCode();
			}
			if(sender.hashCode() == hashcode_a){
				update_freq(lastReceivedOffer , 0);
			}
			else if(sender.hashCode() == hashcode_b){
				update_freq(lastReceivedOffer , 1);
			}
		}

		if (act instanceof Offer ) {
			if(hashcode_a == 0) {
				// Agent A is the first agent to make an offer
				hashcode_a = sender.hashCode();
			}
			else if (hashcode_b == 0) {
				// Agent B is the second agent to make an offer
				hashcode_b = sender.hashCode();
			}
			Offer offer = (Offer) act;
			lastReceivedOffer = offer.getBid();
			//System.out.format("\nReceived Offer with Utility %f from Agent %s\n", 
			//this.utilitySpace.getUtility(lastReceivedOffer), offer.getAgent().getName());
			//lastReceivedOfferDetails = new BidDetails(lastReceivedOffer, 
			//		this.utilitySpace.getUtility(lastReceivedOffer), 
			//		getTimeLine().getTime());
			if(sender.hashCode() == hashcode_a){
				// Store the offer in Agent A's History and Update the Frequencies
				//agentAhistory.add(lastReceivedOfferDetails);
				update_freq(lastReceivedOffer , 0);
			}
			else if(sender.hashCode() == hashcode_b){
				// Store the offer in Agent B's History
				//agentBhistory.add(lastReceivedOfferDetails);
				update_freq(lastReceivedOffer , 1);
			}
		}
	}

	public void update_freq (Bid curr_bid_freq, int agent_num){

		java.util.List<Issue> bid_issues; 
		java.util.HashMap<java.lang.Integer,Value> 	bid_values;
		java.util.List<ValueDiscrete> issue_values;
		IssueDiscrete lIssueDiscrete ;
		ValueDiscrete lValueDiscrete;
		bid_issues = curr_bid_freq.getIssues();
		bid_values = curr_bid_freq.getValues();
		// System.out.println("OFFER");


		if(agent_num == 0){
			for (Integer curr_key : bid_values.keySet()){
				lIssueDiscrete = (IssueDiscrete) (bid_issues.get(curr_key -1));
				lValueDiscrete = (ValueDiscrete) bid_values.get(curr_key);
				//System.out.format("key %d  value string %s value num %d \n ", curr_key , lValueDiscrete.getValue() , lIssueDiscrete.getValueIndex(lValueDiscrete.getValue())); 
				freq_a [curr_key-1][lIssueDiscrete.getValueIndex(lValueDiscrete.getValue())] ++;
			}
		}
		else if(agent_num == 1){
			for (Integer curr_key : bid_values.keySet()){
				lIssueDiscrete = (IssueDiscrete) (bid_issues.get(curr_key -1));
				lValueDiscrete = (ValueDiscrete) bid_values.get(curr_key);
				//System.out.format("key %d  value string %s value num %d \n ", curr_key , lValueDiscrete.getValue() , lIssueDiscrete.getValueIndex(lValueDiscrete.getValue())); 
				freq_b [curr_key-1][lIssueDiscrete.getValueIndex(lValueDiscrete.getValue())] ++;
			}
		}

		int value_index , issue_idx;
		issue_idx = 0;
		//System.out.println("Printing frequency a");
		for(Issue lIssue : bid_issues){
			lIssueDiscrete = (IssueDiscrete)  lIssue; //current issue
			issue_values  = lIssueDiscrete.getValues();     
			//System.out.format("Issue %s\n" , lIssue.convertToString());
			for (ValueDiscrete value : issue_values) {
				value_index = lIssueDiscrete.getValueIndex(value.getValue());
				//System.out.format("FREQ A Value %s Index %d Frequency %d \n\n", value.getValue() , value_index , opponentA.frequency[issue_idx][value_index]);
				//System.out.format("FREQ B Value %s Index %d Frequency %d \n\n", value.getValue() , value_index , freq_b[issue_idx][value_index]);			
			}

			issue_idx ++;
		}
	}

	public Bid generateBid(){
		//Traverse all issues on the Bid ...
		double max_value , curr_value ,randr;
		double curr_time;
		int max_value_idx , num_values , issue_idx;
		Bid generated_bid;
		HashMap<Integer, Value> curr_bid_value = new HashMap<Integer, Value>(); 
		int selected_value;
		generated_bid = null;

		randr =0;

		int N_issues;

		N_issues = domain_issues.size();

		int count_A = N_issues/3;
		int count_B = N_issues/3;
		int our_count = N_issues - count_A - count_B;

		System.out.format("\nAt the beginning, Count A = %d, Count B = %d, Our Count = %d\n", count_A, count_B, our_count);

		curr_time = getTimeLine().getTime();

		System.out.format("\nCurrent Time = %f\n", curr_time);

		for(Issue lIssue : domain_issues) {
			num_values = 0 ;
			issue_idx = 0;
			//System.out.format("MR BEAN INIT: %s number %d \n", .getName() , Element.getNumber() );
			max_value = 0;
			max_value_idx = 0;
			curr_value = 0;
			IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
			evaluator = (EvaluatorDiscrete) (((AdditiveUtilitySpace)this.utilitySpace).getEvaluator(lIssue.getNumber()));
			values = lIssueDiscrete.getValues(); //list of values for this particular issue
			//Get the number of the max value ...
			for (ValueDiscrete value : values ) {
				try{
					curr_value = evaluator.getEvaluation(value);}
				catch (Exception e) {
					e.printStackTrace();
				}
				if(curr_value > max_value){
					max_value = curr_value;
					max_value_idx =  lIssueDiscrete.getValueIndex(value.getValue());
				}	
				num_values ++;
			}

			// Select value according to weights
			double our_weight = additiveUtilitySpace_i.getWeight(lIssue.getNumber());
			double agentA_weight = opponentA.getWeight(lIssue.getNumber()-1);
			double agentB_weight = opponentB.getWeight(lIssue.getNumber()-1);
			if(our_weight > agentA_weight && our_weight > agentB_weight){
				our_count--;
				selected_value = max_value_idx;
				System.out.format("\nGiving issue %d, a value %d which is our maximum\n", lIssue.getNumber(), selected_value);
			}
			else if(agentA_weight > our_weight && agentA_weight > agentB_weight) {
				count_A--;
				selected_value = opponentA.getIssueMaxValueIndex(lIssue.getNumber()-1);
				System.out.format("\nGiving issue %d, a value %d for Agent A\n", lIssue.getNumber(), selected_value);
			}
			else if(agentB_weight > our_weight && agentB_weight > agentA_weight) {
				count_B--;
				selected_value = opponentB.getIssueMaxValueIndex(lIssue.getNumber()-1);
				System.out.format("\nGiving issue %d, a value %d for Agent B\n", lIssue.getNumber(), selected_value);
			}
			else {
				randr = Math.random();
				if(randr < ((count_A)*1.0/N_issues)) {
					count_A--;
					selected_value = opponentA.getIssueMaxValueIndex(lIssue.getNumber()-1);
					System.out.format("\nRandomly Giving issue %d, a value %d for Agent A\n", lIssue.getNumber(), selected_value);
				}
				else if(randr < ((N_issues-our_count)*1.0/N_issues)) {
					count_B--;
					selected_value = opponentB.getIssueMaxValueIndex(lIssue.getNumber()-1);
					System.out.format("\nRandomly Giving issue %d, a value %d for Agent B\n", lIssue.getNumber(), selected_value);
				}
				else {
					our_count--;
					selected_value = max_value_idx;
					System.out.format("\nRandomly Giving issue %d, a value %d which is our maximum\n", lIssue.getNumber(), selected_value);
				}
			}

			curr_bid_value.put(lIssue.getNumber(), lIssueDiscrete.getValue(selected_value));                    
			issue_idx ++;
		}

		generated_bid = new Bid(utilitySpace.getDomain(), curr_bid_value);
		return generated_bid;
	}



	// Method used to decided whether to Accept a Bid or Reject it
	public boolean acceptOrOffer(Bid bid, double target) {
		if(this.utilitySpace.getUtility(bid) < target) {
			return false;
		}
		else {
			return true;
		}
	}

	// Method used to get the target Utility for our Agent based on given paramters and on time
	public double getTargetUtility(double k, double b) {
		return Umax + (Umin - Umax)*(k + (1-k)*Math.pow((getTimeLine().getTime()), b));
	}

	// Method used to get the Bid with most Utility for our Agent
	private Bid getMaxUtilityBid() {
		try {
			return this.utilitySpace.getMaxUtilityBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// Method used to get the Bid with least Utility for our Agent
	private Bid getMinUtilityBid() {
		try {
			return this.utilitySpace.getMinUtilityBid();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// Method to return a Bid whose utility is the average of the best Offers from the other two Agents
	private Bid getAverageBid(){
		double utility_a , utility_b , avg_utility;
		utility_a = agentAhistory.getBestBidDetails().getMyUndiscountedUtil();
		utility_b = agentBhistory.getBestBidDetails().getMyUndiscountedUtil(); 
		avg_utility = ((utility_a + utility_b) /2)*(1+percent_increase);
		return outcome_space.getBidNearUtility(avg_utility).getBid();
	}

	public String getDescription() {
		return description;
	}
}
