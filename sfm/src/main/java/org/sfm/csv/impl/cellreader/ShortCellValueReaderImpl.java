package org.sfm.csv.impl.cellreader;

import org.sfm.csv.ParsingContext;

public final class ShortCellValueReaderImpl implements ShortCellValueReader {

	@Override
	public Short read(char[] chars, int offset, int length, ParsingContext parsingContext) {
		if (length == 0) return null;
		return readShort(chars, offset, length, parsingContext);
	}

	@Override
	public short readShort(char[] chars, int offset, int length, ParsingContext parsingContext) {
		return (short) IntegerCellValueReaderImpl.parseInt(chars, offset, length);
	}

    @Override
    public String toString() {
        return "ShortCellValueReaderImpl{}";
    }
}
