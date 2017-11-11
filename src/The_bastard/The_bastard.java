package The_bastard;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;

import java.util.List;

/**Bastard agent, keeps offering maximum utility bid always. Never accepts anything.
 */
public class The_bastard extends AbstractNegotiationParty {
    private final String description = "The_bastard";

    //private Bid lastReceivedOffer; // offer on the table
    private Bid myBid;
    private double max_value;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
	//myBid = new();
	myBid = this.getMaxUtilityBid();
	max_value = this.utilitySpace.getUtility(myBid);
	System.out.println(myBid.toString());
	//System.out.format("max_value is %f in constructor",max_value);
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

        System.out.println(myBid.toString());
        // First half of the negotiation offering the max utility (the best agreement possible) for Example Agent
        //if (time < 0.5) {
        	max_value = this.utilitySpace.getUtility(myBid);
        	//System.out.format("max_value is %f in constructor",max_value);
            return new Offer(this.getPartyId(), myBid);
	    //} else {

            // Accepts the bid on the table in this phase,
            // if the utility of the bid is higher than Example Agent's last bid.
            //if (lastReceivedOffer != null
	    //   && myLastOffer != null
	    //   && this.utilitySpace.getUtility(lastReceivedOffer) > this.utilitySpace.getUt//ility(myLastOffer)) {

	    //     return new Accept(this.getPartyId(), lastReceivedOffer);
	    // } else {
	    //   // Offering a random bid
		//   myLastOffer = generateRandomBid();
	    //   return new Offer(this.getPartyId(), myLastOffer);
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
            //lastReceivedOffer = offer.getBid();
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
}
