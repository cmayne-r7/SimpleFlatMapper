/**
 * {@link org.sfm.jdbc.JdbcMapper} classes to map object from a csv file. It is instantiated using the {@link org.sfm.jdbc.JdbcMapperFactory} 
 * <p>
 * <code>
 * JdbcMapper&lt;MyObject&gt; jdbcMapper = JdbcMapperFactory.newInstance().newMapper(MyObject.class);<br><br>
 * try (PreparedStatement ps = conn.prepareStatement("select id, email, my_property from MyTable")) {<br>
 * &nbsp;&nbsp;try (ResultSet rs = ps.executeQuery()){<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;jdbcMapper.forEach(rs, (o) -&gt; writer.append(o.toString()).append("\n"));<br>
 * &nbsp;&nbsp;}<br>
 * }
 * </code>
 * </p>
 */
package org.sfm.jdbc;