/**
 * SeeScore Android API
 * Dolphin Computing http://www.dolphin-com.co.uk
 */

package uk.co.dolphin_com.sscore.playdata;

import uk.co.dolphin_com.sscore.SScore;
import uk.co.dolphin_com.sscore.ex.ScoreException;

/**
 * Access to the midi-style play information for the score.<p>
 * iterator returns a BarIterator which steps to each bar in the correct play sequence.
 * The BarIterator returns each Part in the bar and also a virtual metronome part.<p>
 * Part.iterator returns a NoteIterator which steps to each note in the bar in that part.
 * The note returns a midi pitch and start time and duration in ms.
 * 
 * @author J.Sutton Dolphin Computing
 */
public class PlayData implements Iterable<BarIterator.Bar>
{
	/**
	 * the default count-in is 1 bar
	 */
	public static final int kDefaultCountInBars = 1;

	/**
	 * a normal full bar
	 */
	public static final int Bartype_full_bar = 0;
	
	/**
	 * partial bar is first bar in score (ie anacrusis)
	 */
	public static final int Bartype_partial_first_bar = 1;
	
	/**
	 * partial bar including beat 1 (ie before repeat mark)
	 */
	public static final int Bartype_partial_bar_start = 2;
	
	/**
	 * partial bar missing beat 1 (ie after repeat mark)
	 */
	public static final int Bartype_partial_bar_end = 3;
	
	/**
	 * for default argument
	 */
	public static final int Bartype_default = 4;

	/**
	 * construct PlayData
	 * 
	 * @param score the score
	 * @param userTempo an implementation of the UserTempo interface allowing the user eg with a slider
	 * to define the tempo, or tempo scaling
	 * @param countInBars the number of bars to count in before start
	 * @throws ScoreException any error
	 */
	public PlayData(SScore score, UserTempo userTempo, int countInBars) throws ScoreException
	{
		this.score = score;
		this.userTempo = userTempo;
		this.countInBars = countInBars;
		this.nativePointer = getNativePointer(score, userTempo, countInBars);
	}

	/**
	 * construct PlayData with default count-in of 1 bar
	 * 
	 * @param score the score
	 * @param userTempo user-defining tempo instance
	 * @throws ScoreException any error
	 */
	public PlayData(SScore score, UserTempo userTempo) throws ScoreException
	{
		this(score, userTempo, kDefaultCountInBars);
	}

	/**
	  * get the type of the bar 
	  *  
	  * @param barIndex
	  * @return Bartype_*
	  */
	public native int barTypeForBar(int barIndex);

	/**
	 * get the applicable time signature for a particular bar
	 * 
	 * @param barIndex the index of the bar. 0 is the first bar
	 * @return the applicable TimeSig
	 * @throws ScoreException on error
	 */
	public native TimeSig timeSigForBar(int barIndex) throws ScoreException;
	
	/**
	 * get the actual number of beats in the bar and the beat type
	 * This is normally the same as timeSigForBar, but will have fewer beats for a partial bar (eg anacrusis)
	 * 
	 * @param barIndex the index of the bar. 0 is the first bar
	 * @return the effective TimeSig
	 * @throws ScoreException on error
	 */
	public native TimeSig actualBeatsForBar(int barIndex) throws ScoreException;

	/**
	 * get information about any metronome defined in the bar
	 * 
	 * @param barIndex the index of the bar. 0 is the first bar
	 * @return metronome information
	 * @throws ScoreException on error
	 */
	public native Tempo metronomeForBar(int barIndex) throws ScoreException;
	
	/**
	 * get the effective tempo at the bar accounting for any sound tempo elements and metronome elements
	 * 
	 * @param barIndex the index of the bar (0 is 1st)
	 * @return the effective Tempo
	 * @throws ScoreException on error
	 */
	public native Tempo tempoAtBar(int barIndex) throws ScoreException;
	
	/**
	 * get the effective tempo at start of the score accounting for any sound tempo elements and metronome elements
	 * 
	 * @return the effective Tempo
	 * @throws ScoreException on error
	 */
	public native Tempo tempoAtStart() throws ScoreException;
	
	/**
	 * get a beats-per-minute value for a given Tempo and TimeSig
	 * 
	 * @param tempo the score-defined or user-defined tempo
	 * @param timesig the effective time signature
	 * @return beats per minute
	 */
	public native int convertTempoToBPM(Tempo tempo, TimeSig timesig);

	/**
	 * get the number of beats in a bar and the beat timing
	 * 
	 * @param barIndex the index of the bar (0 is 1st)
	 * @param bpm the effective beats-per-minute value
	 * @param bartype one of Bartype_?
	 * @return beat number and timing
	 * @throws ScoreException
	 */
	public native BarBeats getBarBeats(int barIndex, int bpm, int bartype) throws ScoreException;

	/**
	 * does the score define any tempo at the start with metronome or sound tempo elements?
	 * 
	 * @return true if the score defines tempo. Use this to determine whether the user defines
	 *  the absolute tempo or the tempo scaling
	 */
	public native boolean hasDefinedTempo();
	
	/**
	 * get an iterator to the set of bars in the score.
	 * The iterator will start at the first bar (ie count-in bars before start) and sequence through all
	 * bars in playing order accounting for repeats, DC.DS etc
	 * 
	 * @return the iterator
	 */
	public native BarIterator iterator();

	/**
	 * get the number of playing parts in the score
	 * 
	 * @return the number of playing parts
	 */
	public native int numParts();

	/**
	 * get the maximum value of any sound dynamic in any bar. This allows note dynamic values to be scaled accordingly
	 * 
	 * @return the maximum dynamic
	 */
	public native float maxSoundDynamic();
	
	/**
	 * is the first bar an 'up-beat' or anacrusis partial bar?
	 * 
	 * @return true if the first bar is missing the first beat (anacrusis)
	 */
	public native boolean firstBarAnacrusis();
	
	/**
	 * call this on changing the user tempo to tell the interface to re-read the UserTempo and update the
	 * note timings returned 
	 */
	public native void updateTempo();

	protected native void finalize();
	private static native long getNativePointer(SScore score, UserTempo userTempo, int countInBars);
	private final SScore score;
	private final UserTempo userTempo;
	private final int countInBars;
	private final long nativePointer;
}