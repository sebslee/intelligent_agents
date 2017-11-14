package mrBean;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.boaframework.OutcomeSpace;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;

import java.util.List;

/**
 * OurAgent is just a copy of the ExampleAgent for now
 */
public class MrBean extends AbstractNegotiationParty {
    private final String description = "MrBean";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;
    private OutcomeSpace outcome_space;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        outcome_space = new OutcomeSpace(this.utilitySpace);
        outcome_space.generateAllBids(this.utilitySpace);
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
    	myLastOffer = (outcome_space.getBidNearUtility(getTargetUtility(0.5, 3))).getBid();
    	double util = this.utilitySpace.getUtility(myLastOffer);
    	System.out.format("my last offer is %s", myLastOffer);
    	if(lastReceivedOffer == null) {
    		return new Offer(this.getPartyId(), myLastOffer);
    	}
    	if(acceptOrOffer(lastReceivedOffer, util)) {
    		return new Accept(this.getPartyId(), lastReceivedOffer);
    	}
    	else {
    		return new Offer(this.getPartyId(), myLastOffer);
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

        if (act instanceof Offer) { // sender is making an offer
            Offer offer = (Offer) act;

            // storing last received offer
            lastReceivedOffer = offer.getBid();
        }
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
    	return 1.0 - 0.3*(k + (1-k)*Math.pow((getTimeLine().getTime()), 1/b));
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
}
