/**
 * SeeScore For Android
 * Dolphin Computing http://www.dolphin-com.co.uk
 */
/* SeeScoreLib Key for LOOP

 IMPORTANT! This file is for LOOP only.
 It must be used only for the application for which it is licensed,
 and must not be released to any other individual or company.
 */

package uk.co.dolphin_com.sscore;

/**
 * The licence key to enable features in SeeScoreLib supplied by Dolphin Computing
 */

public class LicenceKeyInstance
{
// licence keys: draw, contents, transpose, play_data, item_colour, multipart, android, midi_out
	private static final int[] keycap = {0X1044b5,0X0};
	private static final int[] keycode = {0X325b5ccf,0Xc2a33dd,0X69acb160,0X26b2f868,0X2fd287b1,0Xd83022d6,0X26ca8dfe,0X3c303297,0Xf7c307ea,0X497660,0X56fa98c5,0X5044fbb3,0X5e43da1f,0X38a55992,0Xd7e30130};

	public static final SScoreKey SeeScoreLibKey = new SScoreKey("LOOP", keycap, keycode);
}
