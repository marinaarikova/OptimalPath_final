package net.floodlightcontroller.getstatistic;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.floodlightcontroller.core.types.NodePortTuple;
import io.netty.util.concurrent.ScheduledFuture;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.OLdStatistics;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.threadpool.IThreadPoolService;




public class GetStatistic implements IThreadPoolService, IStatisticsService, IFloodlightModule,IFloodlightService, IOFSwitchListener,IGetStatistic {
	private IStatisticsService statisticcollector;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	protected static Logger logger;
	
	protected IOFSwitchService switchService;
	protected IGetStatistic getstatistic;
		
	
	public void PortStatisticCollection (IStatisticsService statisticcollector) {
		java.util.Map<NodePortTuple, SwitchPortBandwidth> nodeport = statisticcollector.getBandwidthConsumption();        
		for (Entry<NodePortTuple, SwitchPortBandwidth> entry : nodeport.entrySet()) {
		    System.out.println(entry.getKey() + "/" + entry.getValue().getBitsPerSecondRx().getValue() +" "+ entry.getValue().getBitsPerSecondTx().getValue());
		    logger.info(": " + statisticcollector.getBandwidthConsumption());
	    }
	}
	

	@Override
public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	// TODO Auto-generated method stub
	return null;
}


@Override
public java.util.Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	// TODO Auto-generated method stub
	return null;
}


@Override
public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	l.add(IStatisticsService.class);
	return l;
}


@Override
public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
//	switchService.addOFSwitchListener(this);
	statisticcollector.collectStatistics(true);
}

	


@Override
public void init(FloodlightModuleContext context) throws FloodlightModuleException {
	statisticcollector = context.getServiceImpl(IStatisticsService.class);
	statisticcollector.collectStatistics(true);
logger = LoggerFactory.getLogger(OLdStatistics.class);
logger.info(": " + statisticcollector.getBandwidthConsumption());
logger = LoggerFactory.getLogger(OLdStatistics.class);
}


@Override
public SwitchPortBandwidth getBandwidthConsumption(DatapathId dpid, OFPort p) {
	// TODO Auto-generated method stub
	return null;
}


@Override
public java.util.Map<NodePortTuple, SwitchPortBandwidth> getBandwidthConsumption() {
	// TODO Auto-generated method stub
		
return null;

}


@Override
public void collectStatistics(boolean collect) {
	// TODO Auto-generated method stub
	
}


@Override
public void switchAdded(DatapathId switchId) {
	// TODO Auto-generated method stub
	
}


@Override
public void switchRemoved(DatapathId switchId) {
	// TODO Auto-generated method stub
	
}


@Override
public void switchActivated(DatapathId switchId) {
	// TODO Auto-generated method stub
	
}


@Override
public void switchPortChanged(DatapathId switchId, OFPortDesc port, PortChangeType type) {
	// TODO Auto-generated method stub
	
}


@Override
public void switchChanged(DatapathId switchId) {
	// TODO Auto-generated method stub
	
}


@Override
public void switchDeactivated(DatapathId switchId) {
	// TODO Auto-generated method stub
	
}




@Override
public ScheduledExecutorService getScheduledExecutor() {
	// TODO Auto-generated method stub
	return null;
}

}