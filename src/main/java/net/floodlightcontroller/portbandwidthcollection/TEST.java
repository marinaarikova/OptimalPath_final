package net.floodlightcontroller.portbandwidthcollection;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFControllerRole;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFConnection;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.LogicalOFMessageCategory;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.SwitchDescription;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.internal.OFConnection;
import net.floodlightcontroller.core.internal.TableFeatures;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.threadpool.IThreadPoolService;

public class TEST implements  Runnable, IFloodlightModule,IOFSwitchListener, IOFSwitch{
	
	protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT=0;
	protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT=0;
	protected static short FLOWMOD_PRIORITY=101;
	
	public final int DEFAULT_CACHE_SIZE = 10;
	
	private IStatisticsService statisticcollector;
	protected IThreadPoolService threadpool;
	protected IRoutingService routingservice;
	protected IDeviceService deviceservice;
	protected IOFSwitchService switchservice;
	protected static Logger logger;
	protected IFloodlightProviderService floodlightProvider;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	protected String  srchost = "b8:27:eb:75:85:e4";
	protected String  dsthost = "b8:27:eb:cf:54:2c";
	protected String  srchost2 = "b8:27:eb:1b:d9:a3";
	protected String  dsthost2 = "b8:27:eb:9b:9b:ff";
	protected DatapathId dpid;
    protected String SWITCH1 = "1c:48:a8:2b:b5:78:11:a1";
	protected String SWITCH2 = "1c:48:a8:2b:b5:78:11:c1";
	protected String SWITCH3 = "1c:48:a8:2b:b5:78:12:21";
	protected String SWITCH4 = "1c:48:cc:37:ab:fd:1b:41";
	protected MacAddress mac;
	
	

	private void PortStatisticsCollection(IStatisticsService statisticcollector)	{
		
		
		logger.info(": Port Statistic collector is running " + statisticcollector.getBandwidthConsumption());
        Map<NodePortTuple, SwitchPortBandwidth> nodeport = statisticcollector.getBandwidthConsumption();
        for (Entry<NodePortTuple, SwitchPortBandwidth> entry : nodeport.entrySet()) {
        	System.out.println(entry.getKey() + "/" + entry.getValue().getBitsPerSecondRx().getValue() +" "+ entry.getValue().getBitsPerSecondTx().getValue());
        	}
  }

		
	public void request_portstat(DatapathId dpid) {
			
		final Runnable PortStatisticsCollection = new Runnable() {
		public void run() { PortStatisticsCollection(statisticcollector);
		System.out.println("Statistics collection started!");}
		};
		final ScheduledFuture<?> PortStatsHandle =
		scheduler.scheduleAtFixedRate(PortStatisticsCollection , 5, 5, SECONDS);
		scheduler.schedule(new Runnable() {
		public void run() { PortStatsHandle.cancel(true); }
		}, 60 * 60, SECONDS);
	}
		
		//list of switches
		private void printSwitches(DatapathId dpid) {
	        logger.info("SWITCHES");
	        
	        Set<DatapathId> switches = switchservice.getAllSwitchDpids();
			for ( DatapathId sw : switches) {
			
					logger.info("haha="+sw);
			}
		}
		
		// list of devices
//		private List<IDevice> printDevices(String src, String dst) {
//			LinkedList<IDevice> deviceslist = new LinkedList<IDevice>();
				
			public ArrayList<String> client1findSRCattachmentpoints(String srchost){
							ArrayList<String>  srcdeviceslist = new ArrayList<>();
							logger.info("DEVICES");
					logger.info("MACsrc="+srchost.toString());
					
			 IDevice srcdev = deviceservice.findDevice(
					MacAddress.of(srchost),
					VlanVid.ZERO, 
					IPv4Address.NONE,
					IPv6Address.NONE,
					DatapathId.NONE,
					OFPort.ZERO);
			 
			 if (srcdev != null) {
				 
				 SwitchPort[] srcattachmentPoints = srcdev.getAttachmentPoints();
					for (SwitchPort sp : srcattachmentPoints){
						OFPort srcdevPort = sp.getPortId();
						DatapathId srcdevNode = sp.getNodeId();						
						srcdeviceslist.add(0, srcdevNode.toString());		
					    srcdeviceslist.add(1, srcdevPort.toString());
				System.out.println(srcdevNode.toString() + " privet1	" + sp.toString());
						break;
					}
					
				}
			 return srcdeviceslist;
			 }
			
			 
			 
			 public ArrayList<String> server1findDSTattachmentpoints(String dsthost){
					ArrayList<String>  dstdeviceslist = new ArrayList<>();
					logger.info("MACdst="+dsthost.toString());
			IDevice dstdev = deviceservice.findDevice(
					MacAddress.of(dsthost),
					VlanVid.ZERO, 
					IPv4Address.NONE,
					IPv6Address.NONE,
					DatapathId.NONE,
					OFPort.ZERO);
					
		
			if (dstdev != null)
			{		
				
				SwitchPort[] dstattachmentPoints = dstdev.getAttachmentPoints();
				for (SwitchPort sp : dstattachmentPoints){
					OFPort dstdevPort = sp.getPortId();
					DatapathId dstdevNode = sp.getNodeId();				
				dstdeviceslist.add(2, dstdevNode.toString());
				dstdeviceslist.add(3, dstdevPort.toString());
			System.out.println(dstdev.toString() + "privet2" + sp.toString());
					break;
				}
			}
				
		return dstdeviceslist;
		
}
				
	public List<net.floodlightcontroller.routing.Path> getPathsFast(String SWITCH1, String SWITH2){
		
		logger.info("All POSSIBLE PATHS for client1 and server1:" );		
		List<net.floodlightcontroller.routing.Path> allpossiblepaths = routingservice.getPathsFast(DatapathId.of(SWITCH1), DatapathId.of(SWITH2));
//		allpossiblepaths.
		System.out.println(allpossiblepaths.toString());

		return allpossiblepaths;
		}
	
//	public void pushflowtoswitchonpath (Path pathforflow) {
//		
//		 = pathforflow.getPath
//	}
//	
	public ArrayList<String> client2findSRCattachmentpoints(String srchost2){
		ArrayList<String>  srcdeviceslist2 = new ArrayList<>();
		logger.info("DEVICES");
		logger.info("MACsrc2="+srchost2.toString());

		IDevice srcdev2 = deviceservice.findDevice(
				MacAddress.of(srchost2),
				VlanVid.ZERO, 
				IPv4Address.NONE,
				IPv6Address.NONE,
				DatapathId.NONE,
				OFPort.ZERO);

		if (srcdev2 != null) {

			SwitchPort[] srcattachmentPoints = srcdev2.getAttachmentPoints();
			for (SwitchPort sp : srcattachmentPoints){
				OFPort srcdevPort = sp.getPortId();
				DatapathId srcdevNode = sp.getNodeId();						
				srcdeviceslist2.add(4, srcdevNode.toString());		
				srcdeviceslist2.add(5, srcdevPort.toString());
				System.out.println(srcdevNode.toString() + " privet3	" + sp.toString());
				break;
}

}
		return srcdeviceslist2;
}
	
	
		public ArrayList<String> server2findDSTattachmentpoints(String dsthost2){
			ArrayList<String>  dstdeviceslist2 = new ArrayList<>();
			logger.info("MACdst2="+dsthost2.toString());
				IDevice dstdev2 = deviceservice.findDevice(
						MacAddress.of(dsthost2),
						VlanVid.ZERO, 
						IPv4Address.NONE,
						IPv6Address.NONE,
						DatapathId.NONE,
						OFPort.ZERO);
		

				if (dstdev2 != null)
				{		
	
					SwitchPort[] dstattachmentPoints = dstdev2.getAttachmentPoints();
					for (SwitchPort sp : dstattachmentPoints){
						OFPort dstdevPort = sp.getPortId();
						DatapathId dstdevNode = sp.getNodeId();				
						dstdeviceslist2.add(6, dstdevNode.toString());
						dstdeviceslist2.add(7, dstdevPort.toString());
						System.out.println(dstdev2.toString() + "privet4" + sp.toString());
						break;
					}
				}
	
				return dstdeviceslist2;

}
		public List<net.floodlightcontroller.routing.Path> getPathsFast2(String SW1, String SW2){
			
			logger.info("All POSSIBLE PATHS for client2 and server2:" );		
			List<net.floodlightcontroller.routing.Path> allpossiblepaths2 = routingservice.getPathsFast(DatapathId.of(SW1), DatapathId.of(SW2));
				
			System.out.println(allpossiblepaths2.toString());
//			System.out.println(x);

			return allpossiblepaths2;
			}
	
	public void pushflow (DatapathId switchId, OFPort portnumber) {
		
		IOFSwitch sw = switchservice.getSwitch(switchId);
		OFFactory factory = sw.getOFFactory();
		
		//Create a matcher
		Match myMatch = factory.buildMatch()
			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
			    .setExact(MatchField.IP_PROTO, IpProtocol.UDP)
			    .setExact(MatchField.IPV4_DST, IPv4Address.of("192.168.10.55"))
			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("192.168.10.54"))
			    .setExact(MatchField.UDP_DST, TransportPort.of(5566))
			    .build();
		
		//Create action list
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFActions actions = factory.actions();
		
		OFActionOutput output = actions.buildOutput()
			    .setMaxLen(0xFFffFFff)
			    .setPort(portnumber)
			    .build();
		
//		OFActionOutput sentToController = actions.buildOutput()
//			    .setMaxLen(0xFFffFFff)
//			    .setPort(OFPort.CONTROLLER)
//			    .build();
//		
//		OFActionSetQueue sentToQueue = actions.buildSetQueue()
//				.setQueueId(1)
//			    .build();
		
//		actionList.add(sentToQueue);
		actionList.add(output);
		//actionList.add(sentToController);
		
		
		//Create instructions
		ArrayList<OFInstruction> instructionsList = new ArrayList<OFInstruction>();
		OFInstructions instructions = factory.instructions();
		
		OFInstructionApplyActions applyActions = instructions.buildApplyActions()
			    .setActions(actionList)
			    .build();
		
		instructionsList.add(applyActions);
		
		//Build flow entry
		
		
		OFFlowAdd flowAdd13 = factory.buildFlowAdd()
					.setBufferId(OFBufferId.NO_BUFFER)
				    .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
				    .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
				    .setCookie(U64.of(0x7777))
				    .setPriority(32768)
				    .setMatch(myMatch)
			        .setInstructions(instructionsList)
			        .build();
		
		sw.write(flowAdd13);
		System.out.println("Flow entry is pushed");
		
	}
	
		
	
	
	
	
	public void PATH_findrequest(DatapathId dpid) {
		
		final Runnable getPathsFast = new Runnable() {
		public void run() { getPathsFast(SWITCH1, SWITCH2);
		System.out.println("Query for all existing paths started!");}
		};
		final ScheduledFuture<?> allpathHandle =
		scheduler.scheduleAtFixedRate(getPathsFast , 5, 5, SECONDS);
		scheduler.schedule(new Runnable() {
		public void run() {  allpathHandle.cancel(true); }
		}, 60 * 60, SECONDS);
	}
	
public void PATH_findrequest2(DatapathId dpid) {
		
		final Runnable getPathsFast2 = new Runnable() {
		public void run() { getPathsFast2(SWITCH1, SWITCH2);
		System.out.println("Query for all existing paths started2!");}
		};
		final ScheduledFuture<?> allpathHandle =
		scheduler.scheduleAtFixedRate(getPathsFast2 , 5, 5, SECONDS);
		scheduler.schedule(new Runnable() {
		public void run() {  allpathHandle.cancel(true); }
		}, 60 * 60, SECONDS);
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
		
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFSwitchService.class);
		l.add(IFloodlightProviderService.class);
		l.add(IRoutingService.class);
		l.add(IDeviceService.class);
		l.add(IThreadPoolService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		logger = LoggerFactory.getLogger(TEST.class);
		logger.info(" test  :" );
		statisticcollector = context.getServiceImpl(IStatisticsService.class);
		statisticcollector.collectStatistics(true);
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchservice = context.getServiceImpl(IOFSwitchService.class);
		 deviceservice = context.getServiceImpl(IDeviceService.class);
		 threadpool = context.getServiceImpl(IThreadPoolService.class);
		 routingservice = context.getServiceImpl(IRoutingService.class);
	

	 
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		statisticcollector.collectStatistics(true);
		switchservice.addOFSwitchListener(this);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean write(OFMessage m) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<OFMessage> write(Iterable<OFMessage> msgList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R extends OFMessage> ListenableFuture<R> writeRequest(OFRequest<R> request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <REPLY extends OFStatsReply> ListenableFuture<List<REPLY>> writeStatsRequest(OFStatsRequest<REPLY> request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SwitchStatus getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getBuffers() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<OFActionType> getActions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<OFCapabilities> getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<TableId> getTables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SwitchDescription getSwitchDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketAddress getInetAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPortDesc> getEnabledPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPort> getEnabledPortNumbers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFPortDesc getPort(OFPort portNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFPortDesc getPort(String portName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPortDesc> getPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<OFPortDesc> getSortedPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean portEnabled(OFPort portNumber) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean portEnabled(String portName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Date getConnectedSince() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DatapathId getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Object, Object> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public OFControllerRole getControllerRole() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean attributeEquals(String name, Object other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setAttribute(String name, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object removeAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFFactory getOFFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableList<IOFConnection> getConnections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean write(OFMessage m, LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<OFMessage> write(Iterable<OFMessage> msglist, LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OFConnection getConnectionByCategory(LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <REPLY extends OFStatsReply> ListenableFuture<List<REPLY>> writeStatsRequest(OFStatsRequest<REPLY> request,
			LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R extends OFMessage> ListenableFuture<R> writeRequest(OFRequest<R> request,
			LogicalOFMessageCategory category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TableFeatures getTableFeatures(TableId table) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short getNumTables() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public U64 getLatency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		// TODO Auto-generated method stub
		PortStatisticsCollection(statisticcollector);
		request_portstat(switchId);
		printSwitches(switchId);
		this.client1findSRCattachmentpoints(srchost);
		this.server1findDSTattachmentpoints(dsthost);
		PATH_findrequest(dpid);
		PATH_findrequest2(dpid);
    	printSwitches(dpid);
    	this.getPathsFast(SWITCH1, SWITCH2);
    	this.getPathsFast(SWITCH2, SWITCH1);
    	this.client2findSRCattachmentpoints(srchost2);
    	this.client2findSRCattachmentpoints(dsthost2);
    	this.getPathsFast2(SWITCH1, SWITCH2);
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchActivated(DatapathId switchId) {
		
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

}
