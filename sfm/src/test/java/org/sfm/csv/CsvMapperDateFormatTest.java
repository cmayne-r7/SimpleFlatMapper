package org.sfm.csv;

import org.junit.Test;
import org.sfm.map.FieldMapperErrorHandler;
import org.sfm.map.MappingException;
import org.sfm.utils.ListCollectorHandler;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class CsvMapperDateFormatTest {

	public static class ObjectWithDate {
		private final Date date1;
		private Date date2;
		private Date date3;
		public ObjectWithDate(Date date1) {
			this.date1 = date1;
		}
		public Date getDate2() {
			return date2;
		}
		public void setDate2(Date date2) {
			this.date2 = date2;
		}
		public Date getDate1() {
			return date1;
		}

		public Date getDate3() {
			return date3;
		}

		public void setDate3(Date date3) {
			this.date3 = date3;
		}
	}
	@Test
	public void testSetCustomDateFormat() throws ParseException, MappingException, IOException {
		String format = "dd/MM/yyyy HH:mm";
		
		CsvMapper<ObjectWithDate> mapper = CsvMapperFactory.newInstance().defaultDateFormat(format).newMapper(ObjectWithDate.class);
		
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		String strDate = sdf.format(new Date());
		Date date = sdf.parse(strDate);
		
		String data = "date1,date2\n" + strDate + "," + strDate;
		
		
		List<ObjectWithDate> list = mapper.forEach(new StringReader(data), new ListCollectorHandler<ObjectWithDate>()).getList();
		assertEquals(1, list.size());
		
		assertEquals(date, list.get(0).date1);
		assertEquals(date, list.get(0).date2);
	}
	
	@Test
	public void testErrorHandlerAsm() throws ParseException, MappingException, IOException {
		
		@SuppressWarnings("unchecked")
		FieldMapperErrorHandler<CsvColumnKey> fieldMapperErrorHandler = mock(FieldMapperErrorHandler.class);
		CsvMapper<ObjectWithDate> mapper = CsvMapperFactory.newInstance().fieldMapperErrorHandler(fieldMapperErrorHandler).newMapper(ObjectWithDate.class);
		
		String data = "date3,date1,date2\nwrong date,wrong date,wrong date";
		List<ObjectWithDate> list = mapper.forEach(new StringReader(data), new ListCollectorHandler<ObjectWithDate>()).getList();
		assertEquals(1, list.size());
		
		assertNull(list.get(0).date1);
		assertNull(list.get(0).date2);
		assertNull(list.get(0).date3);

		verify(fieldMapperErrorHandler).errorMappingField(eq(new CsvColumnKey("date3", 0)), any(), isNull(), any(Exception.class));
		verify(fieldMapperErrorHandler).errorMappingField(eq(new CsvColumnKey("date1", 1)), any(), isNull(), any(Exception.class));
		verify(fieldMapperErrorHandler).errorMappingField(eq(new CsvColumnKey("date2", 2)), any(), same(list.get(0)), any(Exception.class));
	}

	@Test
	public void testErrorHandlerNoAsm() throws ParseException, MappingException, IOException {

		@SuppressWarnings("unchecked")
		FieldMapperErrorHandler<CsvColumnKey> fieldMapperErrorHandler = mock(FieldMapperErrorHandler.class);
		CsvMapper<ObjectWithDate> mapper = CsvMapperFactory.newInstance().useAsm(false).fieldMapperErrorHandler(fieldMapperErrorHandler).newMapper(ObjectWithDate.class);

		String data = "date3,date1,date2\nwrong date,wrong date,wrong date";
		List<ObjectWithDate> list = mapper.forEach(new StringReader(data), new ListCollectorHandler<ObjectWithDate>()).getList();
		assertEquals(1, list.size());

		assertNull(list.get(0).date1);
		assertNull(list.get(0).date2);
		assertNull(list.get(0).date3);

		verify(fieldMapperErrorHandler).errorMappingField(eq(new CsvColumnKey("date3", 0)), any(), isNull(), any(Exception.class));
		verify(fieldMapperErrorHandler).errorMappingField(eq(new CsvColumnKey("date1", 1)), any(), isNull(), any(Exception.class));
		verify(fieldMapperErrorHandler).errorMappingField(eq(new CsvColumnKey("date2", 2)), any(),same(list.get(0)), any(Exception.class));
	}


}
