/**
 * SeeScore Android API
 * Dolphin Computing http://www.dolphin-com.co.uk
 */

package uk.co.dolphin_com.sscore.playdata;

import java.util.Iterator;

/**
 * Iterator type which iterates through the score returning each Bar in correct play sequence accounting for repeats
 * 
 * @author J.Sutton Dolphin Computing
 */
public class BarIterator implements Iterator<BarIterator.Bar>
{
	/**
	 * Part returns a NoteIterator for a single Bar
	 */
	public class Part implements Iterable<Note>
	{
		/**
		 * @return an iterator to the notes in the bar for this part
		 */
		public native NoteIterator iterator();

		public String toString()
		{
			return " part:" + Integer.toString(partIndex);
		}

		private Part(int partIndex){this.partIndex = partIndex;}
		private final int partIndex;
	}

	/**
	 * Bar returns each Part in the score and also an artificial metronome part which
	 * returns a dummy note for each beat in the bar accounting for time signature and tempo changes
	 */
	public class Bar
	{
		/**
		 * get a Part in the score
		 * 
		 * @param partIndex the part index - 0 is the top part in the score
		 * @return a Part which iterates all the notes in this bar for the part
		 */
		public native Part part(int partIndex);
		
		/**
		 * get an artificial Part for a metronome in this bar
		 * 
		 * @return a Part which iterates artificial metronome notes - 1 per beat in the bar
		 */
		public native Part metronome();
		
		/** 
		 * the index of the bar
		 * 
		 * @return the bar index
		 */
		public native int index();
		
		/**
		 * the duration of this bar accounting for time signature and tempo in force
		 * 
		 * @return the duration in ms
		 */
		public native int duration();
		
		public String toString()
		{
			return " bar:" + Integer.toString(index()) + " duration:" + Integer.toString(duration()) + "ms";
		}
		private Bar() {} // just a wrapper for BarIterator
	}
	

	/**
	 * @return true if this is not the last bar in the score
	 */
	public native boolean hasNext();
	
	/**
	 * @return the next Bar in the score and update this iterator
	 */
	public native Bar next();

	/**
	 * @return false if this is the first bar in the score (ATTN count-in!)
	 */
	public native boolean hasLast();
	
	/**
	 * @return the previous bar in the score and update this iterator
	 */
	public native Bar last();

	/**
	 * @return true if this is a count-in bar (ie before the first bar)
	 */
	public native boolean countIn();
	
	/**
	 * @return the index of this bar in the entire sequence of bars including repeats
	 */
	public native int sequenceIndex();
	
	/**
	 * @return the index of this bar in the score (0 is the first bar)
	 */
	public native int currentBarIndex();
	
	/**
	 * @return the index of the next bar in the score
	 */
	public native int nextBarIndex();

	/**
	 * change to the given bar index. If there is ambiguity because of a repeat it will
	 * use the closest occurrence from the current point in the sequence
	 * 
	 * @param barIndex the index of the bar to move to (0 is the first bar in the score)
	 * @return false if it failed because the bar index was not found
	 */
	public native boolean changeToBar(int barIndex);

	/**
	 * unsupported for immutable list
	 */
	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
	private BarIterator(long nativePointer, int idx) {
		this.nativePointer = nativePointer;
		this.idx = idx;
	}
	private final long nativePointer;
	private final int idx;
}