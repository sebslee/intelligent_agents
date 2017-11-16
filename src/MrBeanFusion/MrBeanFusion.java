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


public class MrBeanFusion extends AbstractNegotiationParty {
    private final String description = "MrBeanFusion";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;
    private Bid maxUtilityOffer;
    private OutcomeSpace outcome_space;
    
    private double Umax = 1.0;
    private double Umin = 0.7;
    
    private int hashcode_a;
    private int hashcode_b;
    
    private double k = 0.2;
    private double b = 0.25;
    
    private BidHistory agentAhistory ;
    private BidHistory agentBhistory ;
    
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        outcome_space = new OutcomeSpace(this.utilitySpace);
        outcome_space.generateAllBids(this.utilitySpace);
        agentAhistory= new BidHistory();
        agentBhistory = new BidHistory();
        hashcode_a = 0;
        hashcode_b = 0;
    }
    
    public Action chooseAction(List<Class<? extends Action>> list) {
        // According to Stacked Alternating Offers Protocol list includes
        // Accept, Offer and EndNegotiation actions only.
        //double util;
    	if(maxUtilityOffer == null){
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
    	
        //util = this.utilitySpace.getUtility(myLastOffer);
    	
        //Every now and then just offer our maximum, depending on time..
        if(Math.random() > getTimeLine().getTime() + 0.3){
            	//myLastOffer = this.getMaxUtilityBid();
    	        return new Offer(this.getPartyId(), maxUtilityOffer);
        }
  
    	if(acceptOrOffer(lastReceivedOffer, (getTargetUtility(k, b)))) {
    		return new Accept(this.getPartyId(), lastReceivedOffer);
    	}
    	else {
    		//return new Offer(this.getPartyId(), myLastOffer);
    		//Return average bid then!
    		myLastOffer =  getAverageBid();
    		System.out.format("MrBean: Target utility %f ",(getTargetUtility(k, b)) );
            return new Offer (this.getPartyId(), getAverageBid());    
    	}
    }
 
    
    public void receiveMessage(AgentID sender, Action act) {
    
        super.receiveMessage(sender, act);
        BidDetails lastReceivedOfferDetails;

        if (act instanceof Offer) { // sender is making an offer
        	if(hashcode_a == 0) {
        		hashcode_a = sender.hashCode();
        	}
        	else if (hashcode_b == 0) {
        			hashcode_b = sender.hashCode();
        	}
            Offer offer = (Offer) act;
            lastReceivedOffer = offer.getBid();
            lastReceivedOfferDetails = new BidDetails(lastReceivedOffer , this.utilitySpace.getUtility(lastReceivedOffer), getTimeLine().getTime() );
            if(sender.hashCode() == hashcode_a) {
            	agentAhistory.add(lastReceivedOfferDetails);
            }
            else if(sender.hashCode() == hashcode_b){
            	agentBhistory.add(lastReceivedOfferDetails);
        	}
		}
    }
    
    //MrBean2 specific methods ..
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
		avg_utility = ((utility_a + utility_b) /2) + 0.1;
	      
		return (  outcome_space.getBidNearUtility(avg_utility).getBid());
	}
	
        public String getDescription() {
        return description;
    }
}