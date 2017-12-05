package group26;

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
import negotiator.bidding.BidDetails ;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Value;
import negotiator.boaframework.OutcomeSpace;
import negotiator.issue.*;
import negotiator.utility.*;
import negotiator.utility.EvaluatorDiscrete;
import java.util.Random;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.lang.ArrayUtils;

public class Agent26 extends AbstractNegotiationParty {

    private final String description = "Agent 26";
	
    // ************************************************************************************
    // *************** Private Class used to predict the opponent model *******************
    // ************************************************************************************
	
    private class OpponentModel {
	private double Issues [][];
	private double Weights [];
	public int frequency [][];
	private int N_issues;
	private int N_values;
	private int maximum_freqs [];
	private int weight_ranking[];
	private int value_ranking[];

	public OpponentModel(int num_issues, int num_values, int [][] freq) {
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
	    //System.out.println("");
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
		}
	    }
	    for(i = 0; i < N_issues; i++) {
		Weights[i] = 2.0*weight_ranking[i]/(N_issues*(N_issues+1.0));
	    }
	}

	public double predictUtility(Bid bid) {
	    double U = 0.0;
	    for(Issue issue: bid.getIssues()) {
		U = U + 
		    Issues[issue.getNumber()-1][((IssueDiscrete) issue).getValueIndex((ValueDiscrete)(bid.getValue(issue.getNumber())))] *
		    Weights[issue.getNumber()-1];
	    }
	    //System.out.format("\n\nPredicted utility = %f", U);
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
	
    // **************************************************************************************************
    // **************************************************************************************************

    private OpponentModel opponentA;
    private OpponentModel opponentB;

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;
    private Bid maxUtilityOffer;
    private Bid minUtilityOffer;

    private double Umax;
    private double Umin;

    private double k;
    private double b;

    private int hashcode_a;
    private int hashcode_b;
    private int number_of_issues; // introduced this to aid in simulated annealing process ..

    private 	int max_num_of_values;
    
    Bid curr_bid ;

    private java.util.List<Issue> domain_issues; 
    private java.util.List<ValueDiscrete> values;
    private EvaluatorDiscrete evaluator;

    // Two dimensional array, number of issues times number of max number of values ...
    int freq_a [][];  
    int freq_b [][];
    double max_weight = 0;
    double min_weight = 1;
    AdditiveUtilitySpace additiveUtilitySpace_i;
    int max_weight_number =1 ;
    double panic; 
    @Override
    public void init(NegotiationInfo info) {
	super.init(info);
	panic = 0.99;	
	// Initialize the Agent's histories and the hashcodes to 0
	hashcode_a = 0;
	hashcode_b = 0;
	curr_bid = this.getMaxUtilityBid(); ;
	// Get the Max and Min Utility Offers
	maxUtilityOffer = this.getMaxUtilityBid();
	minUtilityOffer = this.getMinUtilityBid();
	// Initialize the parameters used to compute the target utility
	Umax = 1.0;
	Umin = (this.utilitySpace.getUtility(minUtilityOffer)+Umax)/2.0;
	k = 0.2;
	b = 1.5;
	// Get the Domain Issues
	domain_issues = this.utilitySpace.getDomain().getIssues();
	number_of_issues = domain_issues.size();
	double curr_weight;

	// Cast the Utility Space into an Additive Utility Space to have access to its methods
	additiveUtilitySpace_i = (AdditiveUtilitySpace)this.utilitySpace;


	max_num_of_values = 0;

	for(Issue lIssue : domain_issues) {
	    IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
	    evaluator = (EvaluatorDiscrete) (((AdditiveUtilitySpace)this.utilitySpace).getEvaluator(lIssue.getNumber()));
	    values = lIssueDiscrete.getValues();
	    curr_weight = (additiveUtilitySpace_i.getWeight(lIssue.getNumber()));
	    if(curr_weight < min_weight) {
		min_weight = curr_weight;
	    }
	    if(curr_weight > max_weight){
		max_weight_number = lIssue.getNumber();
		max_weight = curr_weight;
	    }	
	    if(values.size() > max_num_of_values){
		max_num_of_values = values.size();
	    }
	}

	freq_a = new int [number_of_issues][max_num_of_values];
	freq_b = new int [number_of_issues][max_num_of_values];

	opponentA = new OpponentModel(number_of_issues, max_num_of_values, freq_a);
	opponentB = new OpponentModel(number_of_issues, max_num_of_values, freq_b);

	//Lets initialize the array 
	for(int i = 0 ; i < number_of_issues ; i ++){
	    for(int j = 0 ; j < max_num_of_values ; j++){
		freq_a [i][j] = 0;
		freq_b [i][j] = 0;
	    }
	}

    }


    public Action chooseAction(List<Class<? extends Action>> list) {

	// According to Stacked Alternating Offers Protocol list includes
	// Accept, Offer and EndNegotiation actions only.
	if(lastReceivedOffer == null) {
	    //Lets start with our maximum because we are bad boys
	    myLastOffer = maxUtilityOffer;                
	    return new Offer(this.getPartyId(), myLastOffer);
	}

	//Every now and then just offer our maximum, depending on time..


	// Compute the target utility
	double util;
	util = this.getTargetUtility(k, b);


	//*If the offer is good enough for us or we've reached panic, then accept the offer...
	if(acceptOrOffer(lastReceivedOffer, util) ) {
	    return new Accept(this.getPartyId(), lastReceivedOffer);
	}
	else if (getTimeLine().getTime() > panic ){
	    return new Accept(this.getPartyId(), lastReceivedOffer);
	}

	else {
	    opponentA.updateModel();
	    opponentB.updateModel();
            myLastOffer = generateGoodBid();
	    //70% of the time we generate a "good enough bid", this is to collect enough data to predict the nash offer ...
	    if(getTimeLine().getTime() < 0.5){
		myLastOffer = generateGoodBid();
	    }                      
            else if (model_valid(0) && model_valid (1)){		
		myLastOffer = generateNashBid();
		if(this.utilitySpace.getUtility(myLastOffer) < Umin){
		    myLastOffer = generateGoodBid();
		}
	    }
	    else {
		System.out.println("ASSHOLE DETECTED");
		myLastOffer = generateGoodBid();
	    }
	    
	    return new Offer (this.getPartyId(), myLastOffer);    
	}
    }


    public void receiveMessage(AgentID sender, Action act) {

	super.receiveMessage(sender, act);

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


	if (act instanceof Offer) {
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
	    if(sender.hashCode() == hashcode_a){
		update_freq(lastReceivedOffer , 0);
	    }
	    else if(sender.hashCode() == hashcode_b){
		update_freq(lastReceivedOffer , 1);
	    }
	}
    }


    // So if there is not a significant difference in each issue max and min value, then we return true...
    private boolean model_valid(int agent){
	int max_value, min_value;
	double  difference;
	if(agent == 0){
	    for(int i = 0 ; i < domain_issues.size() ; i ++){
		max_value  = 0 ;
		min_value = 20000000;
		for(int j = 0 ; j < max_num_of_values ; j++){
		    if(freq_a[i][j] > max_value){
			max_value = freq_a[i][j];
		    }
		    if(freq_a[i][j] < min_value){
			min_value = freq_a[i][j];
		    }			       
		}
		difference = ((max_value - min_value) * 100 )/ max_value;
		//Less than 20% of difference, then something is really wrong on the model ...
		if(difference < 20){	    
		    return false;}

	    }
	    return true;
	}
	else{
	    for(int i = 0 ; i < domain_issues.size() ; i ++){
		max_value  = 0 ;
		min_value = 20000000;
		for(int j = 0 ; j < max_num_of_values ; j++){
		    if(freq_b[i][j] > max_value){
			max_value = freq_b[i][j];
		    }
		    if(freq_b[i][j] < min_value){
			min_value = freq_b[i][j];
		    }			       
		}
		difference = ((max_value - min_value) * 100 )/ max_value;
		//Less than 20% of difference, then something is really wrong on the model ...
		if(difference < 20){	    
		    return false;}

	    }	    
	}
	return true;
    }
    
    //Update frequency array used to predict opponet model ...
    public void update_freq (Bid curr_bid_freq , int agent_num){

	java.util.List<Issue> bid_issues; 
	java.util.HashMap<java.lang.Integer,Value> 	bid_values;
	java.util.List<ValueDiscrete> issue_values;
	IssueDiscrete lIssueDiscrete ;
	ValueDiscrete lValueDiscrete;
	bid_issues = curr_bid_freq.getIssues();
	bid_values = curr_bid_freq.getValues();

	if(agent_num == 0){
	    for (Integer curr_key : bid_values.keySet()){
		lIssueDiscrete = (IssueDiscrete) (bid_issues.get(curr_key -1));
		lValueDiscrete = (ValueDiscrete) bid_values.get(curr_key);
		freq_a [curr_key-1][lIssueDiscrete.getValueIndex(lValueDiscrete.getValue())] ++;
	    }
	}
	else if(agent_num == 1){
	    for (Integer curr_key : bid_values.keySet()){
		lIssueDiscrete = (IssueDiscrete) (bid_issues.get(curr_key -1));
		lValueDiscrete = (ValueDiscrete) bid_values.get(curr_key);
		freq_b [curr_key-1][lIssueDiscrete.getValueIndex(lValueDiscrete.getValue())] ++;
	    }
	}

	int value_index , issue_idx;
	issue_idx = 0;
	for(Issue lIssue : bid_issues){
	    lIssueDiscrete = (IssueDiscrete)  lIssue; //current issue
	    issue_values  = lIssueDiscrete.getValues();     
	    for (ValueDiscrete value : issue_values) {
		value_index = lIssueDiscrete.getValueIndex(value.getValue());			
	    }

	    issue_idx ++;
	}
    }

    public Bid generateGoodBid(){
	//Traverse all issues on the Bid ...
	double max_value , curr_value ,randr;
	double curr_time;
	int max_value_idx , num_values , issue_idx;
	Random randomnr = new Random();
	Bid generated_bid;
	HashMap<Integer, Value> curr_bid_value = new HashMap<Integer, Value>(); 
	int selected_value;
	generated_bid = null;
	int agent_max_val_idx , agent_max_val , curr_max_agent_value;
	randr =0;

	curr_time = getTimeLine().getTime();

	for(Issue lIssue : domain_issues) {
	    num_values = 0 ;
	    issue_idx = 0;
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

	    //Throw a coin on the re-normalized weight ...
	    if(additiveUtilitySpace_i.getWeight(lIssue.getNumber()) != max_weight){
		if(Math.random() >  0.1 + (additiveUtilitySpace_i.getWeight(lIssue.getNumber())  - min_weight )/ (max_weight - min_weight)) 
		    {

			selected_value = randomnr.nextInt(num_values);  
		    }

		else {
		    selected_value = max_value_idx;
		}
	    }
	    else {
		selected_value = max_value_idx;
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

    // Method used to get the target Utility for our Agent based on given parameters and on time
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


    private Bid generateNashBid(){

	double To ,  kmax , curr_energy , next_energy , our_utility, predicted_b , predicted_a , a , b , alpha , T ;
	Bid altered_bid;
	int random_issue_1 , random_issue_2;
	HashMap<Integer, Value> curr_bid_value = new HashMap<Integer, Value>(); //auxiliary list for building the bid..
	java.util.HashMap<java.lang.Integer,Value> 	bid_values;
	java.util.List<ValueDiscrete> issue_values;

	a = 0.3;
	b = 0.3;

	alpha = 0.1;

	To = 26;

	kmax = 4000; // Tune this parameter , # of iterations....

	curr_bid = getMaxUtilityBid(); // we will start with max utility bid ....
	//altered_bid = new Bid();
	Random randomnr = new Random();
   
	for(int k = 0 ; k < kmax ; k++){
	    our_utility = this.utilitySpace.getUtility(curr_bid);
	    //Used to model uncertainty of the model ...
	    if(Math.random() > 0.5){
		predicted_a = opponentA.predictUtility(curr_bid) + 0.1 * opponentA.predictUtility(curr_bid) ;}
	    else{
		predicted_a = opponentA.predictUtility(curr_bid) - 0.1 * opponentA.predictUtility(curr_bid) ;
	    }

	    if(Math.random() > 0.5){
		predicted_b = opponentB.predictUtility(curr_bid) + 0.1 * opponentB.predictUtility(curr_bid) ;}
	    else{
		predicted_b = opponentB.predictUtility(curr_bid) - 0.1 * opponentB.predictUtility(curr_bid) ;
	    }
	    //predicted_b = 1;
	    T =  To * Math.pow(alpha , k);
	    //altered_bid = curr_bid;
	    bid_values = curr_bid.getValues();
	    //Randomly select an issue and alter it ...
	    random_issue_1 = randomnr.nextInt(number_of_issues+1);
	    curr_energy = our_utility * predicted_a  * predicted_b +
		a * (1.0/Math.max(0.01, Math.abs(our_utility - predicted_a))) +
		b * (1.0/Math.max(0.01, Math.abs(our_utility - predicted_b))) ;
			    
			
	    for(Issue lIssue : domain_issues){
		IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
		if( random_issue_1 != lIssue.getNumber() ){
		    curr_bid_value.put(lIssue.getNumber() , bid_values.get(lIssue.getNumber()));}
		//alter one single issue putting a random value on it ...
		else {
		    curr_bid_value.put(lIssue.getNumber() , lIssueDiscrete.getValue(randomnr.nextInt( lIssueDiscrete.getNumberOfValues())));
		}


	    }

	    altered_bid = new Bid(utilitySpace.getDomain(), curr_bid_value);
	    our_utility = this.utilitySpace.getUtility(altered_bid);
	    if(Math.random() > 0.5){
		predicted_a = opponentA.predictUtility(altered_bid) + 0.1 * opponentA.predictUtility(altered_bid) ;}
	    else{
		predicted_a = opponentA.predictUtility(altered_bid) - 0.1 * opponentA.predictUtility(altered_bid) ;
	    }

	    if(Math.random() > 0.5){
		predicted_b = opponentB.predictUtility(altered_bid) + 0.1 * opponentB.predictUtility(altered_bid) ;}
	    else{
		predicted_b = opponentB.predictUtility(altered_bid) - 0.1 * opponentB.predictUtility(altered_bid) ;
	    }
			
	    next_energy = our_utility * predicted_a  * predicted_b +
		a * (1.0/Math.max(0.01, Math.abs(our_utility - predicted_a))) +
		b * (1.0/Math.max(0.01, Math.abs(our_utility - predicted_b)));


	    //If P(E(s), E(snew), T) ≥ random(0, 1):
	    //s ← snew
	    // exp(-(e'-e)/T)
	   
			
	    if(
	       this.utilitySpace.getUtility(altered_bid) > getTargetUtility( 0.1 , 1.5) ){
		if(next_energy > curr_energy ){

		    curr_bid = new Bid(altered_bid);
		}

		//P = Math.exp(-(curr_energy - next_energy)/T);

		else if ( Math.exp(  (  next_energy - curr_energy)/T) > Math.random()){
		    curr_bid = new Bid( altered_bid);
		}    
	    }

	}
		

	return curr_bid;
    } 


    // Method to return a Bid whose utility is the average of the best Offers from the other two Agents

    public String getDescription() {
	return description;
    }
}
