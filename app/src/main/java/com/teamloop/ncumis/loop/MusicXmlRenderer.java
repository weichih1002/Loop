package com.teamloop.ncumis.loop;

import nu.xom.Attribute;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

public class MusicXmlRenderer
{
    private Element root;			//	top-level node of entire MusicXML Document
    private Element elCurMeasure;	//	notes, etc. are added to this measure
    private Element elPartList;		//	may need to add score-parts to this
    private Element elCurScorePart;	//	may need to add instruments to this
    private Element elCurPart;		//	current 'voice' add measures to this
    private static final int MUSICXMLDIVISIONS = 4;	//	4 divisions per quarter note



    /*
    *   Generate a default MusicXmlRenderer with score-partwise and identification
    *   default composer is Loop musicXmlRenderer
    * */
    public MusicXmlRenderer()
    {
        root = new Element("score-partwise");
        root.addAttribute(new Attribute("version", "3.0"));

        Element elID = new Element("identification");
        Element elCreator = new Element("creator");
        elCreator.addAttribute(new Attribute("type","composer"));
        elCreator.appendChild("Loop MusicXmlRenderer");
        elID.appendChild(elCreator);
        root.appendChild(elID);

        //	add an empty score-part list here (before any parts are added)
        //	score-parts are added to this as they are generated
        elPartList = new Element("part-list");
        root.appendChild(elPartList);
    }

    /**
     * creates the internal <code>Document</code> with the top-level
     * <code>Element</code> and then creates the MusicXML file (as a
     * string) from the internal <code>Document</code>
     * @return the completed MusicXML file as a String
     */
    public String getMusicXMLString()
    {	Document xomDoc = getMusicXMLDoc();
        return xomDoc.toXML();
    }


    /**
     * creates the internal <code>Document</code> with the top-level
     * <code>Element</code>.
     * @return the completed MusicXML file as a <code>Document</code>
     */
    public Document getMusicXMLDoc()
    {
        finishCurrentVoice();

        //	remove empty measures
        Elements elDocParts = root.getChildElements("part");
        for (int xP = 0; xP < elDocParts.size(); ++xP)
        {
            Element elDocPart = elDocParts.get(xP);
            Elements elPartMeasures = elDocPart.getChildElements("measure");
            for (int xM = 0; xM < elPartMeasures.size(); ++xM)
                if (elPartMeasures.get(xM).getChildCount() < 1)
                    elDocPart.removeChild(xM);
        }

        //	create the Document
        Document xomDoc = new Document(root);
        DocType docType = new DocType("score-partwise",
                "-//Recordare//DTD MusicXML 3.0 Partwise//EN",
                "http://www.musicxml.org/dtds/partwise.dtd");
        xomDoc.insertChild(docType, 0);
        return xomDoc;
    }	//	GetMusicXMLDoc


    // 新增一個音軌(part)進去part-list
    public void newVoice()
    {
        elCurScorePart = new Element("score-part");
        Attribute atPart = new Attribute("id", "P1");
        elCurScorePart.addAttribute(atPart);
        //	empty part name - Finale ignores it and Sibelius gets it wrong
        elCurScorePart.appendChild(new Element("part-name"));

        // add score-part to part-list
        Element elPL = root.getFirstChildElement("part-list");
        elPL.appendChild(elCurScorePart);

        //	start a new part - note that the score-part and the part have the
        //	same id attribute
        elCurPart = new Element("part");
        Attribute atPart2 = new Attribute(atPart);
        elCurPart.addAttribute(atPart2);
        elCurMeasure = null;
        doFirstMeasure(true);   // new first measure
    }	//	newVoice


    //  format the first measure
    public void doFirstMeasure(boolean bAddDefaults)
    {
        if (elCurMeasure == null)
        {
            elCurMeasure = new Element("measure");
            elCurMeasure.addAttribute(new Attribute("number", Integer.toString(1)));
            elCurMeasure.addAttribute(new Attribute("width", Integer.toString(200)));

            //  set the print attributes
            Element elPrint = new Element("print");
            if(bAddDefaults)
            {
                Element topSysDistance = new Element("top-system-distance");
                topSysDistance.appendChild("150");
                elPrint.appendChild(topSysDistance);
            }
            //  add the print tag to the first measure
            if(elPrint.getChildCount() > 0)
                elCurMeasure.appendChild(elPrint);

            //	assemble the attributes element
            Element elAttributes = new Element("attributes");
            if (bAddDefaults)
            {
                //	divisions = 4 per beat
                Element elDivisions = new Element("divisions");
                elDivisions.appendChild(Integer.toString(MUSICXMLDIVISIONS));
                elAttributes.appendChild(elDivisions);

                // 	beats = 1 beat per measure
                Element elTime = new Element("time");
                Element elBeats = new Element("beats");
                elBeats.appendChild(Integer.toString(4));
                elTime.appendChild(elBeats);
                Element elBeatType = new Element("beat-type");
                elBeatType.appendChild(Integer.toString(4));
                elTime.appendChild(elBeatType);
                elAttributes.appendChild(elTime);
            }
            if (bAddDefaults)
            {
                //	Clef - assumed to be treble clef
                Element elClef = new Element("clef");
                Element elSign = new Element("sign");
                elSign.appendChild("G");
                Element elLine = new Element("line");
                elLine.appendChild("2");
                elClef.appendChild(elSign);
                elClef.appendChild(elLine);
                elAttributes.appendChild(elClef);
            }
            //	add the attributes to the measure
            if (elAttributes.getChildCount() > 0)
                elCurMeasure.appendChild(elAttributes);


            // default set a 4 beat rest at the first measure
            Element firstMeasureNote = new Element("note");
            if(bAddDefaults)
            {
                Element firstNoteRest = new Element("rest");
                firstNoteRest.addAttribute(new Attribute("measure", "yes"));
                Element firstNoteDuration = new Element("duration");
                firstNoteDuration.appendChild(Integer.toString(16));
                Element firstNoteVoice = new Element("Voice");
                firstNoteVoice.appendChild(Integer.toString(1));
                firstMeasureNote.appendChild(firstNoteRest);
                firstMeasureNote.appendChild(firstNoteDuration);
                firstMeasureNote.appendChild(firstNoteVoice);
            }
            // add the first measure note to the measure
            if(firstMeasureNote.getChildCount() > 0)
                elCurMeasure.appendChild(firstMeasureNote);

            if (bAddDefaults)
                doTempo(80);	//	80 BMP default
        }
    }	//	doFirstMeasure




    // set the music tempo
    private void doTempo(int tempo)
    {
        Element elDirection = new Element("direction");
        Element elSound = new Element("sound");
        elSound.addAttribute(new Attribute("tempo", Integer.toString(tempo)));
        elDirection.appendChild(elSound);
        //	attach the whole thing to the current measure
        if (elCurMeasure == null)
            doFirstMeasure(true);
        elCurMeasure.appendChild(elDirection);      // default is doTempo in firstMeasure
    }	//	doTempo


    // finish current part voice and append current part to the root
    private void finishCurrentVoice()
    {
        String sCurPartID = (elCurPart == null)             // if current part not null
                ? null                                      // sCurPartID = elCurPart's id value
                : elCurPart.getAttribute("id").getValue();
        boolean bCurVoiceExists = false;
        Elements elParts = root.getChildElements("part");   // get how many 'part' element append from root
        Element elExistingCurPart = null;

        // 如果現在這個part已經出現過了 則新part取代舊part
        for (int x = 0; x < elParts.size(); ++x)
        {
            Element elP = elParts.get(x);
            String sPartID = elP.getAttribute("id").getValue();

            if (sPartID.compareTo(sCurPartID) == 0)     // check if sPartID == sCurPartID
            {
                bCurVoiceExists = true;
                elExistingCurPart = elP;
            }
        }

        //	finish the current measure
        if (elCurPart != null)
        {
            finishCurrentMeasure();

            //  若part沒出現過則append to root
            //  出現過就把舊的取代掉
            if (bCurVoiceExists)
                root.replaceChild(elExistingCurPart, elCurPart);
            else
                root.appendChild(elCurPart);
        }
    }	//	finishCurrentVoice

    public void measureEvent()
    {
        // first measure stuff
        if (elCurMeasure == null)
            doFirstMeasure(false);
        else
        {
            //	add the current measure to the part
            finishCurrentMeasure();
            newMeasure();
        }
    }	//	measureEvent


    // finish current measure and append current measure to elCurPart
    private void finishCurrentMeasure()
    {
        /*	如果現在這個measure 沒有parent
        *    就把他append到elCurPart
        *    如果有parent 且 還和現在measure同number則取代掉舊measure
        */
        if (elCurMeasure.getParent() == null)
            elCurPart.appendChild(elCurMeasure);
        else
        {
            int sCurMNum = Integer.parseInt(elCurMeasure.getAttributeValue("number"));
            Elements elMeasures = elCurPart.getChildElements("measure");
            for (int x = 0; x < elMeasures.size(); ++x)
            {
                Element elM = elMeasures.get(x);
                int sMNum = Integer.parseInt(elM.getAttributeValue("number"));
                if (sMNum == sCurMNum)
                    elCurPart.replaceChild(elM, elCurMeasure);
            }
        }
    }	//	finishCurrentMeasure

    private void newMeasure()
    {
        Integer nextNumber = 1;
        boolean bNewMeasure = true;
        // 	if there aren't any notes in the measure,
        //	continue to use the current measure
        Elements elMeasures = elCurPart.getChildElements("measure");
        Element elLastMeasure;
        if (elMeasures.size() > 0)
        {
            elLastMeasure = elMeasures.get(elMeasures.size()-1);

            //	get the new measure number from the last one
            Attribute elNumber = elLastMeasure.getAttribute("number");
            if (elLastMeasure.getChildElements("note").size() < 1)
                bNewMeasure = false;
            else
                nextNumber = Integer.parseInt(elNumber.getValue()) + 1;
        }
        else
        {
            //	first measure may not have been added yet
            bNewMeasure = (elCurMeasure.getChildElements("note").size() > 0);
        }
        if (bNewMeasure)
        {
            //	start the new measure
            elCurMeasure = new Element("measure");

            //	add the new measure number
            elCurMeasure.addAttribute(new Attribute("number",
                    Integer.toString(nextNumber)));
        }
        //	else continue using the same elCurMeasure
    }	//	newMeasure

    public void changeSystemEvent(boolean change)
    {
        doChangeSystem(change);
    }

    private void doChangeSystem(boolean change)
    {
        Element elPrint = new Element("print");

        // if must change system then add attribute new-system="yes"
        if(change)
        {
            elPrint.addAttribute(new Attribute("new-system","yes"));
        }
        else{
            elPrint.addAttribute(new Attribute("new-system","no"));
        }

        // add to elCurMeasure
        if (elCurMeasure == null)
            doFirstMeasure(true);
        elCurMeasure.appendChild(elPrint);
    }


    public void noteEvent(String value, int octave, int beat)
    {
        doNote(value, octave, beat);
    }

    private void doNote(String value, int octave, int beat)
    {
        String s[] = value.split("n");
        String pitch = s[0];
        String shape = s[1];

        Element elNote = new Element("note");

        //	rest
        if (pitch.equals("rest"))
        {
            Element elRest = new Element("rest");
            elNote.appendChild(elRest);
        }
        else
        {
            //	assemble element - pitch
            Element elPitch = new Element("pitch");
            //	step - note letter name without sharp or flat
            Element elStep = new Element("step");
            elStep.appendChild(pitch);
            elPitch.appendChild(elStep);

            //  compare the shape(alter)
            //  alter - -1 = flat, 1 = sharp
            if (shape.equals("\u266F"))
            {
                int alter = 1;
                Element elAlter = new Element("alter");
                elAlter.appendChild(Integer.toString(alter));
                elPitch.appendChild(elAlter);
            }

            //	octave
            Element elOctave = new Element("octave");
            elOctave.appendChild(Integer.toString(octave));
            elPitch.appendChild(elOctave);

            elNote.appendChild(elPitch);
        }

        //	element duration
        Element elDuration = new Element("duration");
        int duration = 0;
        if(beat != 0)
            duration = 4 * (MUSICXMLDIVISIONS / beat);

        elDuration.appendChild(Integer.toString(duration));
        elNote.appendChild(elDuration);


        //	duration type setting
        String noteType = "";
        if (duration == 4)
            noteType = "quarter";
        else if (duration == 2)
            noteType = "eighth";
        else if (duration == 1)
            noteType = "16th";

        Element elType = new Element("type");
        elType.appendChild(noteType);
        elNote.appendChild(elType);

        //  set stem
        Element elStem = new Element("stem");
        elStem.appendChild("up");
        elNote.appendChild(elStem);

        //	set Voice
        Element elVoice = new Element("voice");
        elVoice.appendChild(Integer.toString(1));
        elNote.appendChild(elVoice);


        if (elCurMeasure == null)
            doFirstMeasure(true);
        elCurMeasure.appendChild(elNote);
    }	//	doNote
}
