package net.floodlightcontroller.ofquality;

import org.projectfloodlight.openflow.types.IpProtocol;

public class Flow {

	protected int id = 0;
	protected String srcip;
	protected String dstip;
	protected IpProtocol proto;
	protected int dstport;
	protected int queue;
	protected boolean applied=false;

	public Flow(int c){
		this.id=c;
	}
	public String getSrcip() {
		return srcip;
	}
	public void setSrcip(String srcip) {
		this.srcip = srcip;
	}
	public String getDstip() {
		return dstip;
	}
	public void setDstip(String dstip) {
		this.dstip = dstip;
	}
	public IpProtocol getProto() {
		return proto;
	}
	public void setProto(IpProtocol proto) {
		this.proto = proto;
	}
	public int getDstport() {
		return dstport;
	}
	public void setDstport(int dstport) {
		this.dstport = dstport;
	}	
	public int getQueue() {
		return queue;
	}
	public void setQueue(int queue) {
		this.queue = queue;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public boolean isApplied() {
		return applied;
	}
	public void setApplied(boolean applied) {
		this.applied = applied;
	}
	
	@Override
	public String toString(){
		return this.srcip+" "+this.dstip+" "+this.proto+" "+this.dstport;
	}
	
	@Override
	public boolean equals(Object o) {
		Flow f = (Flow) o;
		return (o instanceof Flow) && (f.toString()).equals(this.toString());
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
