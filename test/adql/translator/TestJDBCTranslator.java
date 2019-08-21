package adql.translator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import adql.db.DBType;
import adql.db.FunctionDef;
import adql.db.STCS.Region;
import adql.parser.ADQLParser;
import adql.parser.ADQLParser.ADQLVersion;
import adql.parser.grammar.ParseException;
import adql.query.IdentifierField;
import adql.query.operand.ADQLOperand;
import adql.query.operand.NumericConstant;
import adql.query.operand.StringConstant;
import adql.query.operand.function.DefaultUDF;
import adql.query.operand.function.geometry.AreaFunction;
import adql.query.operand.function.geometry.BoxFunction;
import adql.query.operand.function.geometry.CentroidFunction;
import adql.query.operand.function.geometry.CircleFunction;
import adql.query.operand.function.geometry.ContainsFunction;
import adql.query.operand.function.geometry.DistanceFunction;
import adql.query.operand.function.geometry.ExtractCoord;
import adql.query.operand.function.geometry.ExtractCoordSys;
import adql.query.operand.function.geometry.IntersectsFunction;
import adql.query.operand.function.geometry.PointFunction;
import adql.query.operand.function.geometry.PolygonFunction;
import adql.query.operand.function.geometry.RegionFunction;

public class TestJDBCTranslator {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testTranslateOffset() {
		JDBCTranslator tr = new AJDBCTranslator();
		ADQLParser parser = new ADQLParser(ADQLVersion.V2_1);

		try {

			// CASE: Only OFFSET
			assertEquals("SELECT *\nFROM foo\nOFFSET 10", tr.translate(parser.parseQuery("Select * From foo OffSet 10")));

			// CASE: Only OFFSET = 0
			assertEquals("SELECT *\nFROM foo\nOFFSET 0", tr.translate(parser.parseQuery("Select * From foo OffSet 0")));

			// CASE: TOP + OFFSET
			assertEquals("SELECT *\nFROM foo\nLIMIT 5\nOFFSET 10", tr.translate(parser.parseQuery("Select Top 5 * From foo OffSet 10")));

			// CASE: TOP + ORDER BY + OFFSET
			assertEquals("SELECT *\nFROM foo\nORDER BY id ASC\nLIMIT 5\nOFFSET 10", tr.translate(parser.parseQuery("Select Top 5 * From foo Order By id Asc OffSet 10")));

		} catch(ParseException pe) {
			pe.printStackTrace(System.err);
			fail("Unexpected failed query parsing! (see console for more details)");
		} catch(Exception e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate a query with offset into SQL.");
		}
	}

	@Test
	public void testTranslateStringConstant() {
		JDBCTranslator tr = new AJDBCTranslator();

		/* Ensure the translation from ADQL to SQL of strings is correct ;
		 * particularly, ' should be escaped otherwise it would mean the end of
		 * a string in SQL (the way to escape a such character is by doubling
		 * the character '): */
		try {
			assertEquals("'SQL''s translation'", tr.translate(new StringConstant("SQL's translation")));
		} catch(TranslationException e) {
			e.printStackTrace(System.err);
			fail("There should have been no problem to translate a StringConstant object into SQL.");
		}
	}

	@Test
	public void testTranslateUserDefinedFunction() {
		JDBCTranslator tr = new AJDBCTranslator();

		try {
			FunctionDef def = FunctionDef.parse("foo(truc VARCHAR, bidule INT) -> SMALLINT");

			DefaultUDF udf = new DefaultUDF("foo", new ADQLOperand[]{ new StringConstant("hello"), new NumericConstant(1495) });
			udf.setDefinition(def);
			assertNotNull(udf.getDefinition());

			// TEST: NO SQL translation template => same as in ADQL:
			try {
				assertEquals("foo('hello', 1495)", tr.translate(udf));
			} catch(TranslationException e) {
				e.printStackTrace();
				fail("There should have been no problem to translate this UDF. (see console for more details)");
			}

			/* TEST: WITH SQL translation template => follow the template (and
			 *       replace all parameters): */
			def.setSQLTranslationTemplate("left($$1, $$2)");
			try {
				assertEquals("left('hello', 1495)", tr.translate(udf));
			} catch(TranslationException e) {
				e.printStackTrace();
				fail("There should have been no problem to translate this UDF. (see console for more details)");
			}

			// TEST: do not use all parameters => OK
			def.setSQLTranslationTemplate("$$1 || ' world :)'");
			try {
				assertEquals("'hello' || ' world :)'", tr.translate(udf));
			} catch(TranslationException e) {
				e.printStackTrace();
				fail("There should have been no problem to translate this UDF. (see console for more details)");
			}

			/* TEST: use more than 10 parameters (to ensure there is no
			 *       incorrect replacement due to bad detection of $$1 and
			 *       $$10) => OK */
			def = FunctionDef.parse("foo(p1 INT, p2 INT, p3 INT, p4 INT, p5 INT, p6 INT, p7 INT, p8 INT, p9 INT, p10 INT, p11 INT) -> DOUBLE");

			udf = new DefaultUDF("foo", new ADQLOperand[]{ new NumericConstant(1), new NumericConstant(2), new NumericConstant(3), new NumericConstant(4), new NumericConstant(5), new NumericConstant(6), new NumericConstant(7), new NumericConstant(8), new NumericConstant(9), new NumericConstant(10), new NumericConstant(11) });
			udf.setDefinition(def);
			assertNotNull(udf.getDefinition());

			def.setSQLTranslationTemplate("($$1+$$2+$$3)*($$4+$$5+$$6+$$7+$$8+$$9)/($$10*$$11)");

			try {
				assertEquals("(1+2+3)*(4+5+6+7+8+9)/(10*11)", tr.translate(udf));
			} catch(TranslationException e) {
				e.printStackTrace();
				fail("There should have been no problem to translate this UDF. (see console for more details)");
			}

		} catch(ParseException pe) {
			pe.printStackTrace();
			fail("Failed initialization due to an invalide UDF name! (see console for more details)");
		}
	}

	public final static class AJDBCTranslator extends JDBCTranslator {

		@Override
		public String translate(ExtractCoord extractCoord) throws TranslationException {
			return null;
		}

		@Override
		public String translate(ExtractCoordSys extractCoordSys) throws TranslationException {
			return null;
		}

		@Override
		public String translate(AreaFunction areaFunction) throws TranslationException {
			return null;
		}

		@Override
		public String translate(CentroidFunction centroidFunction) throws TranslationException {
			return null;
		}

		@Override
		public String translate(DistanceFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(ContainsFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(IntersectsFunction fct) throws TranslationException {
			return null;
		}

		@Override
		public String translate(PointFunction point) throws TranslationException {
			return null;
		}

		@Override
		public String translate(CircleFunction circle) throws TranslationException {
			return null;
		}

		@Override
		public String translate(BoxFunction box) throws TranslationException {
			return null;
		}

		@Override
		public String translate(PolygonFunction polygon) throws TranslationException {
			return null;
		}

		@Override
		public String translate(RegionFunction region) throws TranslationException {
			return null;
		}

		@Override
		public boolean isCaseSensitive(IdentifierField field) {
			return false;
		}

		@Override
		public DBType convertTypeFromDB(int dbmsType, String rawDbmsTypeName, String dbmsTypeName, String[] typeParams) {
			return null;
		}

		@Override
		public String convertTypeToDB(DBType type) {
			return null;
		}

		@Override
		public Region translateGeometryFromDB(Object jdbcColValue) throws ParseException {
			return null;
		}

		@Override
		public Object translateGeometryToDB(Region region) throws ParseException {
			return null;
		}

	}

}
