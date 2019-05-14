package peer;

import java.util.Comparator;

/**
 * ChunkComparator class
 */
public class ChunkComparator implements Comparator<Chunk> {

    /**
     * SORT ORDER:
     * -> First the chunks that have greater perceived replication degree than desired replication degree
     * -> Second the chunks that have the same perceived replication degree and desired replication degree
     * -> Third the chunks that have less perceived replication degree than desired replication degree
     * NOTE: Inside each case the chunks are ordered by greater data size
     *
     * @param o1
     * @param o2
     * @return comparison from o1 and o2
     */
    @Override
    public int compare(Chunk o1, Chunk o2) {
        int o1_perceived = o1.getPerceivedRepDegree();
        int o1_desired = o1.getRepDegree();
        int o2_perceived = o2.getPerceivedRepDegree();
        int o2_desired = o2.getRepDegree();

        if((o1_perceived-o1_desired) == (o2_perceived-o2_desired)){
            if(o1_perceived == o2_perceived){
                return (o1.getSize() < o2.getSize()) ? 1 : -1;
            }
            else{
                return (o1_perceived < o2_perceived) ? 1 : -1;
            }
        }
        else{
            return (o1_perceived-o1_desired) > (o2_perceived-o2_desired) ? -1 : 0;
        }
    }
}
