package MrBeanFusion;

import java.util.List;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.parties.NegotiationInfo;
import negotiator.boaframework.OutcomeSpace;


public class MrBeanFusion extends AbstractNegotiationParty {

	private final String description = "MrBeanFusion";

	private Bid lastReceivedOffer; // offer on the table
	private Bid myLastOffer;
	private Bid maxUtilityOffer;
	private Bid minUtilityOffer;
	private Bid maxAgentAOffer;
	private Bid maxAgentBOffer;
	private OutcomeSpace outcome_space;

	private double Umax ;
	private double Umin;

	private double k;
	private double b;

	private double percent_increase;

	private int hashcode_a;
	private int hashcode_b;

	@Override
	public void init(NegotiationInfo info) {
		super.init(info);
		outcome_space = new OutcomeSpace(this.utilitySpace);
		outcome_space.generateAllBids(this.utilitySpace);
		hashcode_a = 0;
		hashcode_b = 0;
		// Initialize the parameters used to compute the target utility
		maxUtilityOffer = this.getMaxUtilityBid();
		minUtilityOffer = this.getMinUtilityBid();
		maxAgentAOffer = minUtilityOffer;
		maxAgentBOffer = minUtilityOffer;
		Umax = 1.0;
		Umin = (this.utilitySpace.getUtility(minUtilityOffer)+Umax)/2.0;
		k = 0.2;
		b = 1.5;
		percent_increase = 0.05;
	}

	public Action chooseAction(List<Class<? extends Action>> list) {
		// According to Stacked Alternating Offers Protocol list includes
		// Accept, Offer and EndNegotiation actions only.

		if(lastReceivedOffer == null) {
			//return new Offer(this.getPartyId(), myLastOffer);
			//Lets start with our maximum because we are bad boys
			//System.out.format("They are calling me!");
			myLastOffer = maxUtilityOffer;                
			return new Offer(this.getPartyId(), myLastOffer);

		}

		double util;
		util = this.getTargetUtility(k, b);
		System.out.format("\nMrBean: Target utility %f \n", util);

		//Every now and then just offer our maximum, depending on time..
		if(getTimeLine().getTime() < 0.3){
			//myLastOffer = this.getMaxUtilityBid();
			System.out.println("\nOffering Maximum Utility Bid");
			System.out.format("\nMaximum Utility is %f\n", this.utilitySpace.getUtility(maxUtilityOffer));
			return new Offer(this.getPartyId(), maxUtilityOffer);
		}

		if(acceptOrOffer(lastReceivedOffer, util) || getTimeLine().getTime() > 0.98) {
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
			if(sender.hashCode() == hashcode_a) {
				if(this.utilitySpace.getUtility(maxAgentAOffer) < this.utilitySpace.getUtility(lastReceivedOffer)) {
					maxAgentAOffer = lastReceivedOffer;
				}
			}
			else if(sender.hashCode() == hashcode_b){
				if(this.utilitySpace.getUtility(maxAgentBOffer) < this.utilitySpace.getUtility(lastReceivedOffer)) {
					maxAgentBOffer = lastReceivedOffer;
				}
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

	// Method used to get the target Utility for our Agent based on given paramteers and on time
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

	private Bid getAverageBid(){

		double utility_a , utility_b , avg_utility;

		utility_a = this.utilitySpace.getUtility(maxAgentAOffer);
		utility_b = this.utilitySpace.getUtility(maxAgentBOffer);

		System.out.format("\nBest Bid made by agent A is %f\n", utility_a);
		System.out.format("\nBest Bid made by agent B is %f\n", utility_b);

		avg_utility = ((utility_a + utility_b) /2.0);

		percent_increase = 2.0 - avg_utility;

		avg_utility = avg_utility*(percent_increase);

		return outcome_space.getBidNearUtility(avg_utility).getBid();
	}

	public String getDescription() {
		return description;
	}
}