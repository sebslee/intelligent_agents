package mrBean;
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

/**

   import java.util.List;


   /**
   * OurAgent is just a copy of the ExampleAgent for now
   */
public class MrBean extends AbstractNegotiationParty {
    private final String description = "MrBean";

    private Bid lastReceivedOffer; // offer on the table
    //private Bid myLastOffer;

    private BidHistory agentAhistory= new BidHistory();
    private BidHistory agentBhistory = new BidHistory();
    
    private OutcomeSpace OutcomeSpace_mrbean;
    
    int hashcode_a;
    int hashcode_b;
    
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
	hashcode_a = 0;
	hashcode_b = 0;
	OutcomeSpace_mrbean = new OutcomeSpace(this.utilitySpace);
        OutcomeSpace_mrbean.generateAllBids(this.utilitySpace);
    }

    /**
     * When this function is called, it is expected that the Party chooses one of the actions from the possible
     * action list and returns an instance of the chosen action.
     *
     * @param list
     * @return
     */
    @Override
    public Action chooseAction(List<Class<? extends Action>> list) {
        // According to Stacked Alternating Offers Protocol list includes
        // Accept, Offer and EndNegotiation actions only.
    	//System.out.print("Mr bean is alive!!!!");
    	//return new Offer(this.getPartyId(), this.getMaxUtilityBid());
		if(hashcode_a != 0 && hashcode_b != 0) {
	   return new Offer (this.getPartyId(), getAverageBid());
		}
    	else{
	    return new Offer(this.getPartyId(), this.getMaxUtilityBid());
	    	}
    }

    /**
     * This method is called to inform the party that another NegotiationParty chose an Action.
     * @param sender
     * @param act
     */
    @Override
    public void receiveMessage(AgentID sender, Action act) {
    
        super.receiveMessage(sender, act);

	       BidDetails lastReceivedOfferDetails;
	   
        if (act instanceof Offer) { // sender is making an offer
	    if(hashcode_a == 0)
		hashcode_a = sender.hashCode();
	    else if (hashcode_b == 0)
		hashcode_b = sender.hashCode();
            Offer offer = (Offer) act;
            lastReceivedOffer = offer.getBid();
            lastReceivedOfferDetails = new BidDetails(lastReceivedOffer , this.utilitySpace.getUtility(lastReceivedOffer), getTimeLine().getTime() );
            if(sender.hashCode() == hashcode_a)
		agentAhistory.add(lastReceivedOfferDetails);
            else if(sender.hashCode() == hashcode_b)
		agentBhistory.add(lastReceivedOfferDetails);
	
		}
    }

    /**
     * A human-readable description for this party.
     * @return
     */
    @Override
    
    public String getDescription() {
        return description;
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
	avg_utility = (utility_a + utility_b) /2;
      
	return (  OutcomeSpace_mrbean.getBidNearUtility(avg_utility).getBid());
	}
}
