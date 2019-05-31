package chord;

/**
 * ChordMaintanance Class
 *
 */
public class ChordMaintenance implements Runnable
{
	/**
	 * The parent Chord object
	 */
	Chord parent = null;
	
	/**
	 * ChordMaintenance's constructor
	 * @param parent
	 */
	public ChordMaintenance(Chord parent)
	{
		this.parent = parent;
	}
	
	@Override
	public void run()
	{
		
//		System.out.println(parent);
		
		parent.stabilize();
		parent.fixFingers();
		parent.checkPredecessor();
		
	}
}
