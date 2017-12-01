package group26;

import java.util.List;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.OutcomeSpace;


public class Agent26 extends AbstractNegotiationParty {
    private final String description = "Group 26 Agent";

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
    
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        // Initialize the outcome_space variable and generate all bids
        outcome_space = new OutcomeSpace(this.utilitySpace);
        outcome_space.generateAllBids(this.utilitySpace);
        // Initialize the Agent's histories and the hashcodes to 0
        hashcode_a = 0;
        hashcode_b = 0;
        agentAhistory= new BidHistory();
        agentBhistory = new BidHistory();
        // Get the Max and Min Utility Offers
        maxUtilityOffer = this.getMaxUtilityBid();
        minUtilityOffer = this.getMinUtilityBid();
        // Initialize the parameters used to compute the target utility
        Umax = 1.0;
        Umin = (this.utilitySpace.getUtility(minUtilityOffer)+Umax)/2.0;
        k = 0.2;
        b = 0.3;
        percent_increase = 0.05;
    }
    
    public Action chooseAction(List<Class<? extends Action>> list) {
        // According to Stacked Alternating Offers Protocol list includes
        // Accept, Offer and EndNegotiation actions only.
    	if(lastReceivedOffer == null) {
    		//Lets start with our maximum because we are bad boys
    		myLastOffer = maxUtilityOffer;                
    		return new Offer(this.getPartyId(), myLastOffer);

    	}
    	
        // Every now and then just offer our maximum, depending on time..
        if(Math.random() > getTimeLine().getTime() + 0.3){
        	return new Offer(this.getPartyId(), maxUtilityOffer);
        }
        
        // Compute the target utility
    	double util;
        util = this.getTargetUtility(k, b);
  
    	if(acceptOrOffer(lastReceivedOffer, util)) {
    		// Accept the received Offer
    		return new Accept(this.getPartyId(), lastReceivedOffer);
    	}
    	else {
    		// Reject the received Offer and make a Counter Offer
    		//Return average bid then!
    		myLastOffer =  getAverageBid();
    		return new Offer (this.getPartyId(), myLastOffer);    
    	}
    }
 
    
    public void receiveMessage(AgentID sender, Action act) {
    
        super.receiveMessage(sender, act);
        BidDetails lastReceivedOfferDetails;

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
            lastReceivedOfferDetails = new BidDetails(lastReceivedOffer, 
            										  this.utilitySpace.getUtility(lastReceivedOffer),
            										  getTimeLine().getTime());
            if(sender.hashCode() == hashcode_a) {
            	// Store the offer in Agent A's History
            	agentAhistory.add(lastReceivedOfferDetails);
            }
            else if(sender.hashCode() == hashcode_b){
            	// Store the offer in Agent B's History
            	agentBhistory.add(lastReceivedOfferDetails);
        	}
		}
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
    	return Umax + (Umin - Umax)*(k + (1-k)*Math.pow((getTimeLine().getTime()), 1/b));
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