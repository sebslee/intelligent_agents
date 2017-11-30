package MrBeanFusion;
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
//import negotiator.BidDetails;
import negotiator.bidding.BidDetails ;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;
import negotiator.timeline.Timeline;
import negotiator.boaframework.OutcomeSpace;
//import negotiator.
import negotiator.issue.Objective;
import negotiator.issue.*;
import negotiator.utility.*;
import negotiator.utility.EvaluatorDiscrete;
import java.util.Random;



public class MrBeanFusion extends AbstractNegotiationParty {

    private final String description = "MrBeanFusion";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;
    private Bid maxUtilityOffer;
    private OutcomeSpace outcome_space;
    
    private double Umax = 1.0;
    private double Umin = 0.7;
    
    private double k = 0.2;
    private double b = 0.3;
    
    private double percent_increase = 0.05;
    
    private int hashcode_a;
    private int hashcode_b;
    
    private BidHistory agentAhistory ;
    private BidHistory agentBhistory ;
    
    private java.util.List<Issue> domain_issues; 
    private java.util.List<ValueDiscrete> values;
    EvaluatorDiscrete evaluator;
    //Two dimmension array, number of issues times number of max number of values ...
    int freq_a [][];  
    int freq_b [][];
    double max_weight = 0;
    AdditiveUtilitySpace additiveUtilitySpace_i;
    
    
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        int max_num_of_values;
        outcome_space = new OutcomeSpace(this.utilitySpace);
        outcome_space.generateAllBids(this.utilitySpace);
        agentAhistory= new BidHistory();
        agentBhistory = new BidHistory();
        hashcode_a = 0;
        hashcode_b = 0;
        domain_issues = this.utilitySpace.getDomain().getIssues();
        System.out.format("Domain has %d issues\n ", domain_issues.size());
        double curr_weight;
  
        
        additiveUtilitySpace_i = (AdditiveUtilitySpace)this.utilitySpace;
        
        max_num_of_values = 0;
        
        for(Issue lIssue : domain_issues) {
          System.out.format("MR BEAN INIT: %s number %d \n", lIssue.getName() , lIssue.getNumber() );
          IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
          evaluator = (EvaluatorDiscrete) (((AdditiveUtilitySpace)this.utilitySpace).getEvaluator(lIssue.getNumber()));
          values = lIssueDiscrete.getValues();
          curr_weight = (additiveUtilitySpace_i.getWeight(lIssue.getNumber()));
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
          
          //Lets initialize tbe array 
          for(int i = 0 ; i < domain_issues.size() ; i ++){
          for(int j = 0 ; j < max_num_of_values ; j++){
            freq_a [i][j] = 0;
            freq_b [i][j] = 0;
          }
          }
          
        }
        
    
    
    public Action chooseAction(List<Class<? extends Action>> list) {
        // According to Stacked Alternating Offers Protocol list includes
        // Accept, Offer and EndNegotiation actions only.
        double util;
    	if(maxUtilityOffer == null){
    			System.out.println("\nOffering Maximum Utility Bid at the beginning");
    	        maxUtilityOffer = this.getMaxUtilityBid();
    	        return new Offer(this.getPartyId(), maxUtilityOffer);
    	}
    	if(lastReceivedOffer == null) {
    		//return new Offer(this.getPartyId(), myLastOffer);
    		//Lets start with our maximum because we are bad boys
    		//System.out.format("They are calling me!");
    		myLastOffer = this.getMaxUtilityBid();                
    		return new Offer(this.getPartyId(), myLastOffer);

    	}
    	
        util = this.getTargetUtility(k, b);
        //System.out.format("\nMrBean: Target utility %f \n", util);
    	
        //Every now and then just offer our maximum, depending on time..
        if(Math.random() > getTimeLine().getTime() + 0.3){
            	//myLastOffer = this.getMaxUtilityBid();
        		//System.out.println("\nOffering Maximum Utility Bid");
        		//System.out.format("\nMaximum Utility is %f\n", this.utilitySpace.getUtility(maxUtilityOffer));
    	        return new Offer(this.getPartyId(), maxUtilityOffer);
        }
  
    	if(acceptOrOffer(lastReceivedOffer, util)) {
    		//System.out.println("\nAccepting Offer");
    		return new Accept(this.getPartyId(), lastReceivedOffer);
    	}
    	else {
    		//return new Offer(this.getPartyId(), myLastOffer);
    		//Return average bid then!
    		//myLastOffer =  getAverageBid();
    		myLastOffer = generateBid();
    		//System.out.format("\nMaking Offer with Average Utility %f\n", this.utilitySpace.getUtility(myLastOffer));
            return new Offer (this.getPartyId(), myLastOffer);    
    	}
    }
 
    
    public void receiveMessage(AgentID sender, Action act) {
    
        super.receiveMessage(sender, act);
        BidDetails lastReceivedOfferDetails;
        
       // System.out.format(" TIME %f\n",getTimeLine().getTime() );
        
        
        if (act instanceof Offer) { // sender is making an offer
        	if(hashcode_a == 0) {
        		hashcode_a = sender.hashCode();
        	}
        	else if (hashcode_b == 0) {
        			hashcode_b = sender.hashCode();
        	}
            Offer offer = (Offer) act;
            lastReceivedOffer = offer.getBid();
            
            //System.out.
            
            
            //System.out.format("\nReceived Offer with Utility %f from Agent %s\n", 
            		//this.utilitySpace.getUtility(lastReceivedOffer), offer.getAgent().getName());
            lastReceivedOfferDetails = new BidDetails(lastReceivedOffer , this.utilitySpace.getUtility(lastReceivedOffer), getTimeLine().getTime() );
            if(sender.hashCode() == hashcode_a) {
            	agentAhistory.add(lastReceivedOfferDetails);
            	update_freq(lastReceivedOffer , 1);
            }
            else if(sender.hashCode() == hashcode_b){
            	agentBhistory.add(lastReceivedOfferDetails);
        	}
		}
    }
    
    //MrBean2 specific methods ..
    
    /*      for (ValueDiscrete value : values) {
            try{
            System.out.format("MR BEAN VALUE : %d %s %f \n" , lIssueDiscrete.getValueIndex(value.getValue()), value.getValue(), evaluator.getEvaluation(value));}
            catch (Exception e) {
            e.printStackTrace();
             }
            }*/
    
    public void update_freq (Bid curr_bid , int agent_num){
    
    java.util.List<Issue> bid_issues; 
    java.util.HashMap<java.lang.Integer,Value> 	bid_values;
    java.util.List<ValueDiscrete> issue_values;
    IssueDiscrete lIssueDiscrete ;
    ValueDiscrete lValueDiscrete;
    bid_issues = curr_bid.getIssues();
    bid_values = curr_bid.getValues();
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
      //System.out.format("Value %s Index %d Frequency %d \n", value.getValue() , value_index , freq_a[issue_idx][value_index]);
    }
    issue_idx ++;
    }
    
    
    
    }
    
    
    public Bid generateBid(){
    //Traverse all issues on the Bid ...
    
           double max_value , curr_value ,randr;
           int max_value_idx , num_values , issue_idx;
           Random randomnr = new Random();
           Bid generated_bid;
           HashMap<Integer, Value> curr_bid_value = new HashMap<Integer, Value>(); 
           int selected_value;
           generated_bid = null;
           int agent_max_val_idx , agent_max_val , curr_max_agent_value;
           randr =0;
           for(Issue lIssue : domain_issues) {
           num_values = 0 ;
        issue_idx = 1;
          //System.out.format("MR BEAN INIT: %s number %d \n", .getName() , Element.getNumber() );
          max_value = 0;
          max_value_idx = 0;
          curr_value = 0;
          IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
          evaluator = (EvaluatorDiscrete) (((AdditiveUtilitySpace)this.utilitySpace).getEvaluator(lIssue.getNumber()));
          values = lIssueDiscrete.getValues(); //list of values for this particuar issue
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
          if(Math.random() > /* (1.7  - getTimeLine().getTime()) */ (Math.log((additiveUtilitySpace_i.getWeight(lIssue.getNumber()) / max_weight) )+1)) {
          
          randr = Math.random();
          if( randr < 1/3){
          //get the max value idx from the freq array of agent ...
          agent_max_val = 0;
          agent_max_val_idx = 0;
          for(int j = 0; j < lIssueDiscrete.getNumberOfValues() ; j++){
          curr_max_agent_value = freq_a [issue_idx][j];
          if(curr_max_agent_value > agent_max_val){
          agent_max_val = curr_max_agent_value;
          agent_max_val_idx = j;
          }
          }
          selected_value = agent_max_val_idx;
          }
          
          else if (randr > 1/3 && randr < 2/3){
                  agent_max_val = 0;
          agent_max_val_idx = 0;
          for(int j = 0; j < lIssueDiscrete.getNumberOfValues() ; j++){
          curr_max_agent_value = freq_a [issue_idx][j];
          if(curr_max_agent_value > agent_max_val){
          agent_max_val = curr_max_agent_value;
          agent_max_val_idx = j;
          }
          }
          selected_value = agent_max_val_idx;
          }
          
          else {          
          //chose random value ...
          selected_value = randomnr.nextInt(num_values);}
          
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
    
    
    
    public boolean acceptOrOffer(Bid bid, double target) {
    	if(this.utilitySpace.getUtility(bid) < target) {
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
    public double getTargetUtility(double k, double b) {
    	return Umax + (Umin - Umax)*(k + (1-k)*Math.pow((getTimeLine().getTime()), 1/b));
    }
    
    private Bid getMaxUtilityBid() {
        try {
            return this.utilitySpace.getMaxUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Bid getAverageBid(){
      	  
		double utility_a , utility_b , avg_utility;
		
		utility_a = agentAhistory.getBestBidDetails().getMyUndiscountedUtil();
		utility_b = agentBhistory.getBestBidDetails().getMyUndiscountedUtil(); 
		
		//System.out.format("\nBest Bid made by agent A is %f\n", utility_a);
		//System.out.format("\nBest Bid made by agent B is %f\n", utility_b);
		
		avg_utility = ((utility_a + utility_b) /2)*(1+percent_increase);
	      
		return outcome_space.getBidNearUtility(avg_utility).getBid();
	}
	
    public String getDescription() {
        return description;
    }
}
