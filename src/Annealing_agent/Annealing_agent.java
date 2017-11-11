package Annealing_agent;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;

import java.util.List;

/**
 * 
 */
public class Annealing_agent extends AbstractNegotiationParty {
    private final String description = "Annealing_agent";

    private Bid lastReceivedOffer; // offer on the table
    private Bid myLastOffer;

    private double floor; // Floor will decrease linearly on time, the lowest we can go is reservation value...
    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        floor = 0.9; //We will go as low as 0.6 initially...
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
        //double time = getTimeLine().getTime(); // Gets the time, running from t = 0 (start) to t = 1 (deadline).
                                               // The time is normalized, so agents need not be
                                               // concerned with the actual internal clock.
        //First round offer max utility bid...                                       
        if(lastReceivedOffer == null) {
        return new Offer(this.getPartyId() , this.getMaxUtilityBid());
        }
        
        if(this.utilitySpace.getUtility(lastReceivedOffer) >= floor  ) 
                        return new Accept(this.getPartyId(), lastReceivedOffer);
else
        return new Offer(this.getPartyId() , generateBid());

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
    
    private Bid generateBid(){
    Boolean offer_generated = false;
    Bid generated_bid;
     do{
    generated_bid = generateRandomBid();
    if ( this.utilitySpace.getUtility(generated_bid) > 0.6 ) {
    floor = floor * Math.exp(getTimeLine().getTime());
    offer_generated = true;
    return generated_bid;
    
    }
    else {
    if ( ((Math.exp(floor - this.utilitySpace.getUtility(generated_bid)))/getTimeLine().getTime() + 0.0001) > Math.random()){
        floor = floor * Math.exp(getTimeLine().getTime());
        offer_generated = true;
        return generated_bid;
    }
    }
    }while(!offer_generated);
    return generated_bid;
    }
    
}


