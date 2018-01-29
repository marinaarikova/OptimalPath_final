package net.floodlightcontroller.optimalpath;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Path;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.threadpool.IThreadPoolService;
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

public class OptimalPath implements IFloodlightModule, IOFSwitchListener, Runnable, IOFMessageListener {

	protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT=0;
	protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT=0;
	protected static short FLOWMOD_PRIORITY=101;
	protected static short FLOWMOD_PRIORITY_1=102;
	
	public final int DEFAULT_CACHE_SIZE = 10;
	
	private IStatisticsService statisticcollector;
	protected IThreadPoolService threadpool;
	protected IRoutingService routingservice;
	protected IDeviceService deviceservice;
	protected IOFSwitchService switchservice;
	protected static Logger logger;
	protected IFloodlightProviderService floodlightProvider;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	protected String src_host1 = "00:1c:f0:fa:df:17";
	protected String dst_host1 = "84:7b:eb:03:70:b6";
	protected String dst_host2 = "00:10:18:33:61:cf";
	protected String src_host2 = "f0:79:59:80:9d:55";
	protected DatapathId dpid;
    protected String SWITCH1 = "1c:48:a8:2b:b5:78:11:a1";
	protected String SWITCH2 = "1c:48:a8:2b:b5:78:11:c1";
	protected String SWITCH3 = "1c:48:a8:2b:b5:78:12:21";
	protected String SWITCH4 = "1c:48:cc:37:ab:fd:1b:41";
	private boolean network_is_started = false;
	public List<Path> all_paths = null;
	public long port_capacity = 838860800 ;
//	private long rx_rate = 0;
//	private long tx_rate = 0;
//	private long link_rate = 0;
	private List<Long> bit_rates = new ArrayList<Long>();
	private List<NodePortTuple> first_path = new ArrayList<NodePortTuple>();
	private List<NodePortTuple> second_path = new ArrayList<NodePortTuple>();
	
	public Map<NodePortTuple, SwitchPortBandwidth> PortStatisticsCollection(IStatisticsService statisticcollector){
		
		Map<NodePortTuple, SwitchPortBandwidth> nodeport = statisticcollector.getBandwidthConsumption();
		System.out.println("I am inside of PortStat for");

		for (Entry<NodePortTuple, SwitchPortBandwidth> entry : nodeport.entrySet()) {        	System.out.println(entry.getKey() + "/" + entry.getValue().getBitsPerSecondRx().getValue() +" "+ entry.getValue().getBitsPerSecondTx().getValue());
       	}
        return nodeport;
	}

	
	public ArrayList<NodePortTuple> getAttachmentPoint(String mac_src, String mac_dst, List<NodePortTuple> node_port_tuple) {
		ArrayList<NodePortTuple> tuple_list = new ArrayList<NodePortTuple>(Arrays.asList(null, null));
		//List<NodePortTuple> tuple_list = new ArrayList<NodePortTuple>();   
		System.out.println(tuple_list.toString());
		
		IDevice dst_dev = deviceservice.findDevice(
				MacAddress.of(mac_dst),
				VlanVid.ZERO, 
				IPv4Address.NONE,
				IPv6Address.NONE,
				DatapathId.NONE,
				OFPort.ZERO);
		IDevice src_dev = deviceservice.findDevice(
				MacAddress.of(mac_src),
				VlanVid.ZERO, 
				IPv4Address.NONE,
				IPv6Address.NONE,
				DatapathId.NONE,
				OFPort.ZERO);
		
		SwitchPort[] dst_attachment_points = dst_dev.getAttachmentPoints();
		SwitchPort[] src_attachment_points = src_dev.getAttachmentPoints();
		NodePortTuple tuple_dst = new NodePortTuple(dst_attachment_points[0].getNodeId(), dst_attachment_points[0].getPortId());
		NodePortTuple tuple_src = new NodePortTuple(src_attachment_points[0].getNodeId(), src_attachment_points[0].getPortId());
		
		tuple_list.set(0, tuple_src);
		tuple_list.set(1, tuple_dst);
		System.out.println(tuple_list.toString());
		return tuple_list;
	}
	
	public void pushflow (String mac_src, String mac_dst, List<NodePortTuple> node_port_tuple, short priority) {
		
		Integer nodeID = 1;
//		List<NodePortTuple> node_port_tuple = path.getPath();
//		IDevice dst_dev = deviceservice.findDevice(
//				MacAddress.of(mac_dst),
//				VlanVid.ZERO, 
//				IPv4Address.NONE,
//				IPv6Address.NONE,
//				DatapathId.NONE,
//				OFPort.ZERO);
//		SwitchPort[] dst_attachment_points = dst_dev.getAttachmentPoints();
//		NodePortTuple tuple = new NodePortTuple(dst_attachment_points[0].getNodeId(), dst_attachment_points[0].getPortId());
//		System.out.println(" before "+ node_port_tuple );
		System.out.println("Flow pusher");
//		node_port_tuple.add(tuple);
//		System.out.println("Flow pusher, NodePortTuple " + node_port_tuple.toString());
		
		for (NodePortTuple node: node_port_tuple){
				IOFSwitch sw = switchservice.getSwitch(node.getNodeId());
				OFFactory factory = sw.getOFFactory();
				//Create a matcher
				Match myMatch = factory.buildMatch()
					    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
					    .setExact(MatchField.ETH_SRC, MacAddress.of(mac_src))
					    .setExact(MatchField.ETH_DST, MacAddress.of(mac_dst))
					    .build();
				
				Match myMatch_back = factory.buildMatch()
					    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
					    .setExact(MatchField.ETH_SRC, MacAddress.of(mac_dst))
					    .setExact(MatchField.ETH_DST, MacAddress.of(mac_src))
					    .build();
				
				//Create action list
				ArrayList<OFAction> actionList = new ArrayList<OFAction>();
				OFActions actions = factory.actions();
				
				OFActionOutput output = actions.buildOutput()
					    .setMaxLen(0xFFffFFff)
					    .setPort(node.getPortId())
					    .build();
				
				actionList.add(output);
				
				//Create instructions
				ArrayList<OFInstruction> instructionsList = new ArrayList<OFInstruction>();
				OFInstructions instructions = factory.instructions();
				
				OFInstructionApplyActions applyActions = instructions.buildApplyActions()
					    .setActions(actionList)
					    .build();
				
				instructionsList.add(applyActions);
				
				if ((nodeID % 2) == 0){
				//Build flow entry
				OFFlowAdd flowAdd13 = factory.buildFlowAdd()
							.setBufferId(OFBufferId.NO_BUFFER)
						    .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
						    .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
						    .setCookie(U64.of(0x7777))
						    .setPriority(priority)
						    .setMatch(myMatch)
					        .setInstructions(instructionsList)
					        .build();
					sw.write(flowAdd13);
					System.out.println("Flow entry for forward direction is pushed: " + node.toString());
					nodeID ++;
				} else {
					OFFlowAdd flowAdd13 = factory.buildFlowAdd()
							.setBufferId(OFBufferId.NO_BUFFER)
						    .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
						    .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
						    .setCookie(U64.of(0x7777))
						    .setPriority(priority)
						    .setMatch(myMatch_back)
					        .setInstructions(instructionsList)
					        .build();
					sw.write(flowAdd13);
					System.out.println("Flow entry backward direction is pushed to point: " + node.toString());
					nodeID ++;
				}
		}
	}
	
	public List<Path> findPath(String mac_src, String mac_dst) {
		
		List<Path> all_paths = new ArrayList<Path>();
		
		IDevice src_dev = deviceservice.findDevice(
				MacAddress.of(mac_src),
				VlanVid.ZERO, 
				IPv4Address.NONE,
				IPv6Address.NONE,
				DatapathId.NONE,
				OFPort.ZERO);

		IDevice dst_dev = deviceservice.findDevice(
				MacAddress.of(mac_dst),
				VlanVid.ZERO, 
				IPv4Address.NONE,
				IPv6Address.NONE,
				DatapathId.NONE,
				OFPort.ZERO);
		
		try{
			//System.out.println("find");
			if ((src_dev != null) && (dst_dev != null)) {
				SwitchPort[] src_attachment_points = src_dev.getAttachmentPoints();
				SwitchPort sp = src_attachment_points[0];
				DatapathId src_dev_node = sp.getNodeId();
				
				SwitchPort[] dst_attachment_points = dst_dev.getAttachmentPoints();
				SwitchPort sp_dst = dst_attachment_points[0];
				DatapathId dst_dev_node = sp_dst.getNodeId();
				
				all_paths = routingservice.getPathsFast(src_dev_node, dst_dev_node);
				System.out.println(all_paths.get(0).toString());
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return all_paths;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		
		return null;
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		if (! network_is_started) {
			if (switchservice.getAllSwitchDpids().size() == 4) {
				if (deviceservice.getAllDevices().size() == 4) {
					System.out.println("starting network");
					all_paths = findPath(src_host1, dst_host1);
					first_path = all_paths.get(0).getPath();
					List<NodePortTuple> att_point = getAttachmentPoint(src_host1, dst_host1, first_path);
					System.out.println(att_point.toString());
					first_path.add(0,att_point.get(0));
					first_path.add(first_path.size(), att_point.get(1));
					pushflow(src_host1, dst_host1, first_path, FLOWMOD_PRIORITY);
					System.out.println("First path before " + first_path.toString());
					att_point = getAttachmentPoint(src_host2, dst_host2, first_path);
					//first_path.remove(first_path.size()-1);
					first_path.set(0, att_point.get(0));
					first_path.set(first_path.size()-1, att_point.get(1));
					pushflow(src_host2, dst_host2, first_path, FLOWMOD_PRIORITY);
					System.out.println("First path after " + first_path.toString());
					network_is_started = true;
				}	
			}
		
		} else {
//			
			//System.out.println("else" + first_path.toString());
			Map<NodePortTuple, SwitchPortBandwidth> port_stat = PortStatisticsCollection(statisticcollector);
			//Integer nodeID = 1;
			//System.out.println(port_stat.toString());
			if (! port_stat.isEmpty()) {
				for (NodePortTuple node: first_path) {
					System.out.println("for each node");
					
					long rx_rate = port_stat.get(node).getBitsPerSecondRx().getValue();
		        	long tx_rate = port_stat.get(node).getBitsPerSecondTx().getValue();
		        	//System.out.println(port_stat.get(node).getBitsPerSecondRx());
		        	
		        	if ((rx_rate + tx_rate) > port_capacity) {
		        		
						System.out.println("Re-route ");
						second_path = all_paths.get(1).getPath();
						System.out.println("Second path before " + second_path.toString());
						List<NodePortTuple> att_point = getAttachmentPoint(src_host2, dst_host2, second_path);
						
						if ((!second_path.contains(att_point.get(0)) || (!second_path.contains(att_point.get(att_point.size()-1))))) {
							second_path.add(0, att_point.get(0));
							second_path.add(second_path.size(), att_point.get(1));
							System.out.println("Re-routed path" + second_path.toString());
							pushflow(src_host2, dst_host2, second_path, FLOWMOD_PRIORITY_1);
							//nodeID ++;
							System.out.println("Second path after " + second_path.toString());
						}
		        	}
				} 
			}	
		}
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		// TODO Auto-generated method stub
				
		//Path path_1 = findPath(src_host1, dst_host1, 0);
		//Path path_2 = findPath(src_host2, dst_host2, 0);
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
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFSwitchService.class);
		l.add(IFloodlightProviderService.class);
		l.add(IRoutingService.class);
		l.add(IDeviceService.class);
		l.add(IThreadPoolService.class);
		l.add(IStatisticsService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		logger = LoggerFactory.getLogger(OptimalPath.class);
		statisticcollector = context.getServiceImpl(IStatisticsService.class);
		//statisticcollector.collectStatistics(true);
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchservice = context.getServiceImpl(IOFSwitchService.class);
		deviceservice = context.getServiceImpl(IDeviceService.class);
		threadpool = context.getServiceImpl(IThreadPoolService.class);
		routingservice = context.getServiceImpl(IRoutingService.class);
		statisticcollector.collectStatistics(true);
		//floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		//floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchservice.addOFSwitchListener(this);
		threadpool.getScheduledExecutor().scheduleWithFixedDelay(this, 10, 5, TimeUnit.SECONDS);
		first_path = null;
		second_path = null;
	}

}
