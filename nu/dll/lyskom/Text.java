/**! -*- Mode: Java; c-basic-offset: 4 -*-
 *
 * Copyright (c) 1999 by Rasmus Sten <rasmus@sno.pp.se>
 *
 */
package nu.dll.lyskom;

import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import java.io.StringReader;



/**
 * Represents a LysKOM text. An application can construct Text objects and use
 * the Session.createText(Text) call to create them on the server. The call
 * Session.getText(int) also returns Text objects. <br>
 * Most information about the text's status can be found in its TextStat object
 * and the corresponding MiscInfo list.
 * 
 * @see nu.dll.lyskom.Text#getStat()
 * @see nu.dll.lyskom.TextStat
 * @see nu.dll.lyskom.Session#getText(int)
 * @see nu.dll.lyskom.Session#createText(Text)
 */
public class Text extends Hollerith implements Serializable {
	private static final long serialVersionUID = -7201392864569389382L;

	boolean cached = false;

    int textNo = -1;
    TextStat stat = null;

    Text setCached(boolean b) {
        cached = b;
        return this;
    }

    /**
     * Returns <tt>true</tt> if this text was retreived from text cache rather
     * than directly from the server
     */
    public boolean isCached() {
        return cached;
    }

    /* constructors */
    /**
     * Creates an empty text.
     */
    public Text() {
        setContents(new byte[] {});
        stat = new TextStat();
    }

    protected Text(int no, byte[] contents) {
        setContents(contents);
        textNo = no;
    }

    protected Text(int textNo) {
        this.textNo = textNo;
    }

    protected Text(int textNo, TextStat stat) {
        this.textNo = textNo;
        this.stat = stat;
    }

    /**
     * Creates a new text with the supplied subject and body, converted into
     * bytes using the default encoding.
     */
    public Text(String subject, String body) {
        this(subject, body, Session.defaultServerEncoding);
    }

    /**
     * Creates a new text with the supplied subject and body, converted into
     * bytes using the supplied encoding and setting the text's encoding
     * properly.
     */
    public Text(String subject, String body, String charset) {
        stat = new TextStat();
        try {
            setCharset(charset);
            setContents((subject + "\n" + body).getBytes(charset));
        } catch (UnsupportedEncodingException ex1) {
            throw new RuntimeException("Unsupported character encoding: "
                    + ex1.getMessage());
        }
    }

    public Text(byte[] contents, String charset) {
        stat = new TextStat();
        setCharset(charset);
        setContents(contents);
    }

    /**
     * Creates a new text with the supplied contents
     */
    public Text(byte[] contents) {
        stat = new TextStat();
        setContents(contents);
    }

    public String getCharset() {
        return stat.getCharset();
    }

    public void setCharset(String charset) {
        super.setCharset(charset);
        stat.setCharset(charset);
    }

    public void setContentType(String type) {
        stat.setContentType(type);
    }

    public String getContentType() {
        String contentTypeString = stat.getFullContentType();
	return contentTypeString;
    }

    public String getName() {
        return "" + textNo;
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getBody());
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException(
                "A text does not provide a DataSource output stream");
    }

    /** end of constructors **/

    /**
     * @see nu.dll.lyskom.TextStat#getLocal(int)
     */
    public int getLocal(int confNo) {
        return stat.getLocal(confNo);
    }

    /**
     * Trims the contents on this text - removes trailing whitespace to follow
     * LysKOM conventions.
     */
    public void trimContents() {
        byte[] contents = getContents();

        if (contents == null || contents.length == 0)
            return;

        int lastchar = 0;
        for (int i = contents.length - 1; i >= 0; i--) {
            if (contents[i] == ' ' || contents[i] == '\n')
                continue;
            lastchar = i;
            break;
        }
        byte[] newcontents = new byte[lastchar + 1];
        for (int i = 0; i < newcontents.length; i++)
            newcontents[i] = contents[i];
        setContents(newcontents);

    }

    /**
     * Count the number of rows this text contains.
     */
    public int getRows() {
        int count = 0;
        for (int i = 0; i < contents.length; i++)
            if (contents[i] == '\n')
                count++;
        return count;
    }

    /**
     * Return the author of this text.
     */
    public int getAuthor() {
        return stat.author;
    }

    /**
     * Return the number of marks on this text.
     */
    public int getMarks() {
        return stat.marks;
    }

    /**
     * Returns the AuxItem data for a given AuxItem tag.
     * 
     * @see nu.dll.lyskom.AuxItem
     */
    public Hollerith[] getAuxData(int tag) {
        return stat.getAuxData(tag);
    }

    public List<AuxItem> getAuxItems(int tag) {
        return stat.getAuxItems(tag);
    }

    /**
     * Adds an array of recipients to this text.
     */
    public Text addRecipients(int[] no) {
        for (int i = 0; i < no.length; i++)
            addRecipient(no[i]);
        return this;
    }

    /**
     * Adds a recipient to this text.
     */
    public Text addRecipient(int no) {
        if (stat.hasRecipient(no)) {
            throw new IllegalArgumentException(no + " is already a recipient");
        }
        addMiscInfoEntry(TextStat.miscRecpt, no);
        return this;
    }

    /**
     * Removes all standard recipients in this text.
     */
    public void clearRecipients() {
        clearMiscInfoEntry(TextStat.miscRecpt);
    }

    /**
     * Removes a recipient from this text.
     */
    public void removeRecipient(int conf) {
        removeMiscInfoEntry(TextStat.miscRecpt, conf);
    }

    /**
     * Removes a CC-recipient from this text.
     */
    public void removeCcRecipient(int conf) {
        removeMiscInfoEntry(TextStat.miscCcRecpt, conf);
    }

    /**
     * Adds an array of recipients as CC-recipients to this text.
     */
    public Text addCcRecipients(int[] no) {
        for (int i = 0; i < no.length; i++)
            addCcRecipient(no[i]);
        return this;
    }

    /**
     * Adds one CC-recipient to this text.
     */
    public Text addCcRecipient(int no) {
        if (stat.hasRecipient(no)) {
            throw new IllegalArgumentException(no + " is already a recipient");
        }
        addMiscInfoEntry(TextStat.miscCcRecpt, no);
        return this;
    }

    /**
     * Clones a text by copying its contents and recipient lists into a new Text
     * object, which is then returnesd.
     */
    public Object clone() {
        Text t = new Text();
        t.setContents(getContents());
        t.addRecipients(getRecipients());
        t.addCcRecipients(getCcRecipients());
        return t;
    }

    /**
     * @deprecated moved to TextStat class
     */
    public void clearMiscInfoEntry(int key) {
        stat.clearMiscInfoEntry(key);
    }

    /**
     * @deprecated moved to TextStat class
     */
    public void removeMiscInfoEntry(int key, int value) {
        stat.removeMiscInfoEntry(key, value);
    }

    /**
     * @deprecated moved to TextStat class
     */
    public void addMiscInfoEntry(int key, int value) {
        stat.addMiscInfoEntry(key, value);
    }

    /**
     * Adds a text-number to which this is a comment.
     */
    public Text addCommented(int no) {
        addMiscInfoEntry(TextStat.miscCommTo, no);
        return this;
    }

    /**
     * Adds a text number to which this is a footnore.
     */
    public Text addFootnoted(int no) {
        addMiscInfoEntry(TextStat.miscFootnTo, no);
        return this;
    }

    /**
     * Returns the subject (first row) of this text.
     */
    public byte[] getSubject() {
        // do not use getContents() since, if we are a BigText object,
        // we don't want to fetch the entire body form the server.
        byte[] b = contents;
        int i = 0;
        while (i < b.length && b[i] != '\n')
            i++;
        if (i >= b.length) {
            return new byte[] {};
        }
        byte[] r = new byte[i];
        for (i = 0; i < r.length; i++)
            r[i] = b[i];
        return r;
    }

    public String getSubjectString() throws UnsupportedEncodingException {
        return new String(getSubject(), getCharset());
    }
    
    public String getSubjectString8() {
    	String value = "SubjectString";
    	try {
			value = new String(getSubject(), "iso-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return value;
    }


    public String getBodyString() throws UnsupportedEncodingException {
        return new String(getBody(), getCharset());
    }

    public String getBodyString8() {
    	String value = "BodyString";
    	
        try {
			value = new String(getBody(), "iso-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return value;
    }

    /**
     * Returns the body (everything but the first row) of this text.
     */
    public byte[] getBody() {
        return getBody(getContents());
    }

    protected byte[] getBody(byte[] contents) {
        int i = 0;
        byte[] b = contents;
        while (i < b.length && b[i] != '\n') {
            i++;
        }
        if (i >= b.length) {
            return b;
        }

        byte[] r = new byte[b.length - i - 1]; // -1 is \n
        i++; // skip '\n'
        System.arraycopy(b, i, r, 0, r.length);
        return r;
    }

    public static List<String> getRows(String s) {
        try {
            BufferedReader rdr = new BufferedReader(new StringReader(s));
            List<String> bodyList = new LinkedList<String>();
            String row;
            while ((row = rdr.readLine()) != null) {
                bodyList.add(row);
            }
            return bodyList;
        } catch (IOException ex1) {
            throw new RuntimeException("This shouldn't happen");
        }
    }

    /**
     * Returns the body of this text as a List of String objects, converted from
     * bytes using this text's encoding.
     */
    public List<?> getBodyList() {
        try {
            return getRows(new String(getBody(), getCharset()));
        } catch (UnsupportedEncodingException ex1) {
            throw new RuntimeException("Unsupported character encoding: "
                    + ex1.getMessage());
        }
    }

    /**
     * Returns an int[] for Misc-Info members with integer values
     * 
     * @deprecated moved to TextStat
     * @see nu.dll.lyskom.TextStat#getStatInts(int)
     */
    public int[] getStatInts(int no) {
        return stat.getStatInts(no);
    }

    /**
     * Returns the time at which this text was created.
     */
    public Date getCreationTime() {
        return getStat().getCreationTime().getTime();
    }

    /**
     * Returns a string represantation of the time at which this text was
     * created.
     */
    public String getCreationTimeString() {
        return getStat().getCreationTime().toString();
    }

    public String toString() {
        return "<TEXT " + getNo() + ": " + contents.length + " bytes"
                + (isCached() ? " (cached)" : "") + ">";
    }

    /**
     * Returns an array containing all recipients for this text.
     */
    public int[] getRecipients() {
        return getStatInts(TextStat.miscRecpt);
    }

    /**
     * Returns an array containing all CC-recipients for this text.
     */
    public int[] getCcRecipients() {
        return getStatInts(TextStat.miscCcRecpt);
    }

    /**
     * Returns an array containing all BCC-recipients for this text.
     */
    public int[] getBccRecipients() {
        return getStatInts(TextStat.miscBccRecpt);
    }

    /**
     * Returns an array containing all the texts of which this is a comment to.
     */
    public int[] getCommented() {
        return stat.getCommented();
    }

    /**
     * Returns an array containing all comments to this text.
     */
    public int[] getComments() {
        return getStatInts(TextStat.miscCommIn);
    }

    /**
     * Returns an array of all texts to which this text is a footnote to.
     */
    public int[] getFootnoted() {
        return getStatInts(TextStat.miscFootnTo);
    }

    /**
     * Returns an array of all footnotes to this text.
     */
    public int[] getFootnotes() {
        return getStatInts(TextStat.miscFootnIn);
    }

    /**
     * Returns an array of all that has sent this text to new recipient. Note
     * that this list is NOT consistent with getRecipients()!
     */
    public int[] getSenders() {
        return getStatInts(TextStat.miscSentBy);
    }

    /**
     * Returns an array of the times when people has added new recipients to
     * this text. Note that this list is NOT consistent with getRecipients()!
     */
    public int[] getSendTimes() {
        return getStatInts(TextStat.miscSentAt);
    }

    /**
     * Sets the TextStat object for this text.
     */
    public void setStat(TextStat stat) {
        this.stat = stat;
    }

    /**
     * Returns the TextStat object containing information about this text's
     * status (such as recipients, footnotes, et.c.). It is the TextStat object
     * you should use to get detailed recipient and sender information.
     */

    public TextStat getStat() {
        return stat;
    }

    /**
     * Returns the number of this text.
     */
    public int getNo() {
        return textNo;
    }

    /**
     * Sets the number of this text.
     */
    protected void setNo(int n) {
        textNo = n;
    }

    public static String wrap(String s, int margin) {
        List<String> rows = getRows(s);
        List<String> newRows = new LinkedList<String>();

        Iterator<String> i = rows.iterator();
        while (i.hasNext()) {
            String row = (String) i.next();
            boolean skip = false;
            while (!skip && row.length() > margin) {
                int cutAt = row.lastIndexOf(' ', margin);
                if (cutAt == -1) { // can't break row
                    skip = true;
                    continue;
                }
                String wrappedRow = row.substring(0, cutAt);
                row = row.substring(cutAt + 1);
                newRows.add(wrappedRow);
            }
            newRows.add(row);
        }

        i = newRows.iterator();
        StringBuffer newBody = new StringBuffer();
        while (i.hasNext()) {
            String row = (String) i.next();
            newBody.append(row + "\n");
        }
        return newBody.toString();
    }

    int rightMargin = Integer.getInteger("lattekom.linewrap", new Integer(70))
            .intValue();

    public String getWrapped() throws UnsupportedEncodingException {
        return wrap(getBodyString(), rightMargin);
    }

}
