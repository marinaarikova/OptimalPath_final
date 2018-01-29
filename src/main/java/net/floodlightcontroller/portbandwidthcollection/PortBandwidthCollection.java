package net.floodlightcontroller.portbandwidthcollection;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.RoutingManager;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class PortBandwidthCollection implements  Runnable, IFloodlightModule{
	
	private IStatisticsService statisticcollector;
	protected IThreadPoolService threadpool;
	protected IRoutingService routingservice;
	protected IDeviceService deviceservice;
	protected IOFSwitchService switchservice;
	protected static Logger logger;
	protected IFloodlightProviderService floodlightProvider;
	  
	
	
	public PortBandwidthCollection (IStatisticsService statisticcollector) {
		java.util.Map<NodePortTuple, SwitchPortBandwidth> nodeport = statisticcollector.getBandwidthConsumption();        
		for (Entry<NodePortTuple, SwitchPortBandwidth> entry : nodeport.entrySet()) {
		    System.out.println(entry.getKey() + "/" + entry.getValue().getBitsPerSecondRx().getValue() +" "+ entry.getValue().getBitsPerSecondTx().getValue());
		    logger.info(": " + statisticcollector.getBandwidthConsumption());
	    }
	}
	
	
	//list of switches
	private void printSwitches() {
        logger.debug("SWITCHES");
		Set<DatapathId> switches = switchservice.getAllSwitchDpids();
		for ( DatapathId sw : switches) {
			logger.debug("SwitchId="+sw);
		}
	}
	
	// list of devices
	private void printDevices(String src, String dst) {
			
		
        logger.debug("DEVICES");
		 IDevice srcdev = deviceservice.findDevice(
				MacAddress.of("b8:27:eb:cf:54:2c"),
				VlanVid.ZERO, 
				IPv4Address.NONE,
				IPv6Address.NONE,
				DatapathId.NONE,
				OFPort.ZERO);
		
		IDevice dstdev = deviceservice.findDevice(
				MacAddress.of("b8:27:eb:75:85:e4"),
				VlanVid.ZERO, 
				IPv4Address.NONE,
				IPv6Address.NONE,
				DatapathId.NONE,
				OFPort.ZERO);
		
//		List<Path> result = routingservice.getPathsFast(srcdev., dstdev);
//		          for (<Path> rs:result) {
//		  			logger.debug("PathsExsit="+rs);
//		  		}
//		        
		
					
			logger.debug("MAC="+srcdev.toString());
			logger.debug("MAC="+dstdev.toString());
			
						}
	 
	
	
	
		private void printDevices() {
        logger.debug("DEVICES");
		Collection<? extends IDevice> devices = deviceservice.getAllDevices();
		for (IDevice device : devices) {
			logger.debug("MAC="+device.getMACAddressString());
		}
	}




	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService >> floodlightService = 
				new ArrayList<Class<? extends IFloodlightService>>();
			floodlightService.add(IFloodlightProviderService.class);
			floodlightService.add(IRoutingService.class);
			floodlightService.add(IDeviceService.class);
			floodlightService.add(IThreadPoolService.class);
			floodlightService.add(IThreadPoolService.class);
			floodlightService.add(IOFSwitchService.class);
			
			return floodlightService;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		statisticcollector = context.getServiceImpl(IStatisticsService.class);
		statisticcollector.collectStatistics(true);
	logger = LoggerFactory.getLogger(PortBandwidthCollection.class);
	logger.info(": " + statisticcollector.getBandwidthConsumption());
	threadpool = context.getServiceImpl(IThreadPoolService.class);
	routingservice = context.getServiceImpl(IRoutingService.class);
	deviceservice = context.getServiceImpl(IDeviceService.class);
	switchservice = context.getServiceImpl(IOFSwitchService.class);
	logger = LoggerFactory.getLogger(RoutingManager.class);
	
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		statisticcollector.collectStatistics(true);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}

	
	
