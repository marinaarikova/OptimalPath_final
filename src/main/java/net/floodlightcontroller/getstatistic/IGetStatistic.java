package net.floodlightcontroller.getstatistic;

import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.statistics.IStatisticsService;

public interface IGetStatistic extends IStatisticsService {
     public void PortStatisticCollection (IStatisticsService statisticcollector);
}
