package org.sfm.csv.parser;

import java.io.IOException;
import java.io.Reader;

public final class CharBuffer {


	private char[] buffer;
	private int bufferSize;

	private int mark;

	private final int maxBufferSize;

	public CharBuffer(final int bufferSize, int maxBufferLength) {
		this.maxBufferSize = maxBufferLength;
		this.buffer = new char[bufferSize];
	}
	
	public void mark(int index) {
		this.mark = index;
	}
	
	public boolean fillBuffer(Reader reader) throws IOException {
		int length = reader.read(buffer, bufferSize, buffer.length - bufferSize);
		if (length != -1) {
			bufferSize += length;
			return true;
		} else {
			return false;
		}
	}


	public int shiftBufferToMark() throws BufferOverflowException {
		// shift buffer consumer data
		int usedLength = Math.max(bufferSize - mark, 0);

		// if buffer tight double the size
		if (usedLength <= (bufferSize >> 1)) {
			System.arraycopy(buffer, mark, buffer, 0, usedLength);
		} else {
			int newBufferSize = Math.min(maxBufferSize, buffer.length << 1);

			if (newBufferSize == usedLength) {
				throw new BufferOverflowException("The content in the csv cell exceed the maxSizeBuffer " + maxBufferSize + ", see CsvParser.DSL.maxSizeBuffer(int) to change the default value");
			}
			// double buffer size
			char[] newBuffer = new char[newBufferSize];
			System.arraycopy(buffer, mark, newBuffer, 0, usedLength);
			buffer = newBuffer;
		}
		bufferSize = usedLength;

		int m = mark;
		mark = 0;
		return m;
	}

	public char[] getCharBuffer() {
		return buffer;
	}


	public int getMark() {
		return mark;
	}



	public char getChar(int bufferIndex) {
		return buffer[bufferIndex];
	}

	public int getBufferSize() {
		return bufferSize;
	}
}
