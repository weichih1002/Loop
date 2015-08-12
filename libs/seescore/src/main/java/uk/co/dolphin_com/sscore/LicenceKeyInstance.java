/**
 * SeeScore For Android
 * Dolphin Computing http://www.dolphin-com.co.uk
 */
package uk.co.dolphin_com.sscore;

/**
 * The licence key to enable features in SeeScoreLib supplied by Dolphin Computing
 */
public class LicenceKeyInstance
{
	// licence keys: contents, transpose, item_colour, draw_multipart, android
	private static final int[] keycap = {0X4494,0X0};
	private static final int[] keycode = {0X614f9a7e,0X3127f81b,0Xbfd8b439,0X3711cdd3,0X71dff3e3,0Xbcc9f574,0X2604f67d,0Xfd29c0b,0X3ecb13c5,0X4c029e4a,0X3fe63342,0Xbf45af9c,0X222f52d4,0X50e6edd4,0X2c66a0e};

	public static final SScoreKey SeeScoreLibKey = new SScoreKey("evaluation", keycap, keycode);
}