package net.floodlightcontroller.ofquality.ovsdb;

import java.util.LinkedList;
import java.util.List;

public class OVSDBOperations{

	List<OVSDBOperationObject> operations = new LinkedList<>();

	public void addOperation(OVSDBOperationObject obj){
		this.operations.add(obj);
	}

	public OVSDBOperationObject getOperation(int i){
		return this.operations.get(i);
	}

	public List<OVSDBOperationObject> getOperation() {
		return operations;
	}

	public void setOperation(List<OVSDBOperationObject> operation) {
		this.operations = operation;
	}
}


