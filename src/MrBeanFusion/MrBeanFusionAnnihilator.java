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
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.OutcomeSpace;


public class MrBeanFusionAnnihilator extends AbstractNegotiationParty {
    private final String description = "MrBeanFusion Annihilator";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;
    private Bid maxUtilityOffer;
    private Bid minUtilityOffer;
    private OutcomeSpace outcome_space;
    
    
    private double Umax = 1.0;
    private double Umin = 0.9;
    
    private double T = 0.1; // Temperature for Simulated Annealing
    
    private double k = 0.2;
    private double b = 0.3;
    
    private double percent_increase = 0.00;
    
    private int hashcode_a;
    private int hashcode_b;
    
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
        double util;
    	if(maxUtilityOffer == null){
    			System.out.println("\nOffering Maximum Utility Bid at the beginning");
    	        maxUtilityOffer = this.getMaxUtilityBid();
    	        minUtilityOffer = this.getMinUtilityBid();
    	        System.out.println("\nIssues in Min Bid:");
    	        for(Integer i: minUtilityOffer.getValues().keySet()) {
    	        	System.out.format("\n%f\n", ((AdditiveUtilitySpace)this.utilitySpace).getEvaluation(i, minUtilityOffer));
    	        }
    	        System.out.println("\nIssues in Max Bid:");
    	        for(Integer i: maxUtilityOffer.getValues().keySet()) {
    	        	System.out.format("\n%f\n", ((AdditiveUtilitySpace)this.utilitySpace).getEvaluation(i, maxUtilityOffer));
    	        }
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
        System.out.format("\nMrBean: Target utility %f \n", util);
    	
        //Every now and then just offer our maximum, depending on time..
        if(Math.random() > getTimeLine().getTime() + 0.3){
            	//myLastOffer = this.getMaxUtilityBid();
        		System.out.println("\nOffering Maximum Utility Bid");
        		System.out.format("\nMaximum Utility is %f\n", this.utilitySpace.getUtility(maxUtilityOffer));
    	        return new Offer(this.getPartyId(), maxUtilityOffer);
        }
  
    	if(acceptOrOffer(this.utilitySpace.getUtility(lastReceivedOffer), util)) {
    		System.out.println("\nAccepting Offer");
    		return new Accept(this.getPartyId(), lastReceivedOffer);
    	}
    	else {
    		//return new Offer(this.getPartyId(), myLastOffer);
    		//Return average bid then!
    		myLastOffer =  getAverageBid();
    		System.out.format("\nMaking Offer with Average Utility %f\n", this.utilitySpace.getUtility(myLastOffer));
            return new Offer (this.getPartyId(), myLastOffer);    
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
            System.out.format("\nReceived Offer with Utility %f from Agent %s\n", 
            		this.utilitySpace.getUtility(lastReceivedOffer), offer.getAgent().getName());
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
    public boolean acceptOrOffer(double offerUtility, double target) {
    	T = 0.9*getTimeLine().getTime()+0.1;
    	if(Math.exp((offerUtility - target)/T) > Math.random()) {
    		return true;
    	}
    	else {
    		return false;
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
    
    private Bid getMinUtilityBid() {
    	try {
            return this.utilitySpace.getMinUtilityBid();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Bid getAverageBid(){
      	  
		double utility_a , utility_b , avg_utility;
		
		utility_a = agentAhistory.getBestBidDetails().getMyUndiscountedUtil();
		utility_b = agentBhistory.getBestBidDetails().getMyUndiscountedUtil(); 
		
		System.out.format("\nBest Bid made by agent A is %f\n", utility_a);
		System.out.format("\nBest Bid made by agent B is %f\n", utility_b);
		
		avg_utility = ((utility_a + utility_b) /2)*(1+percent_increase);
	      
		return outcome_space.getBidNearUtility(avg_utility).getBid();
	}
	
    public String getDescription() {
        return description;
    }
}