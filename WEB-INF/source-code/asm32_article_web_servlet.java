// asm32_article_web_servlet.java

// import org.sqlite.core.DB;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;

public class asm32_article_web_servlet extends HttpServlet {
	public static final String strAppName = "asm32-article-sqlite3-servlet";

	private final static String strDriver="org.sqlite.JDBC";
	private final static String dataFile = "asm32.article.sqlite3";
	private final static String strTableName = "table_article";
	private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final static String pageFormatA = "<html>\n" +
			"<head>\n" +
			"<meta charset=\"utf-8\">\n" +
			"<title>" + strAppName + "</title>\n" +
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n" +
			"\t<style>\n" +
			"\tbody { background-color:#a5cbf7; }\n" +
			"\tul.page { display: block; width:100%; height: 20px; clear:both; }\n" +
			"\tul.page li, .nav { display: block; width:30px; height: 20px; float:left; margin:5px; }\n" +
			"\tul.page li a, .nav a { display: block; width:30px; height: 20px; text-align:center; float:left; border:1px solid #069; border-radius:5px; line-height: 20px; }\n" +
			"\tli { line-height: 25px; }\n" +
			"\t.nav a.modify::after { content:'m'; }\n" +
			"\t</style>\n" +
			"</head>\n" +
			"<body>\n\n\n";

	private final static String pageFormatB = "\n\n</body>\n</html>\n";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();

		Connection conn = null;
		boolean isClosed = true;

		String act = request.getParameter("act");
		String pn = request.getParameter("pn");
		String id = request.getParameter("id");

		out.println(pageFormatA);

		try{

			conn = getConnection(request);
			if( conn == null ){

				out.println( "Conn is null" );

			} else {
				isClosed = false;
				if( id == null ){

					out.println("<h1>" + strAppName + "</h1>\n\n\n");

					out.println( PA_getArticleList(request, conn, pn) );
					out.println( PA_getArticleForm(request, null, null) );

				}else if(act != null && act.equals("m")){

					out.println( PA_getArticleForm(request, conn, id) );

				}else{

					out.println( PA_getArticleDetails(request, conn, id) );

				}
			}


		}catch(Exception ex){

			out.println("Exception: " + ex.getMessage());

		}finally{
			if(!isClosed){
				try{
					conn.close();
				}catch(Exception ex){}
			}
		}

		out.println(pageFormatB);
	}


	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		request.setCharacterEncoding("utf-8");
		response.setContentType("text/html; charset=utf-8");
		response.setCharacterEncoding("utf-8");

		PrintWriter out = response.getWriter();

		Connection conn = null;
		boolean isClosed = true;

		String act = request.getParameter("act");
		String id = request.getParameter("id");

		try{

			conn = getConnection(request);
			if( conn != null ){
				isClosed = false;
			}

			switch(act){
				case "da":
				case "dm":
					out.println( PA_doArticleDetailsUpdate(request, conn, act) );
					break;
				default:
					out.println("act=" + act);
			}

		}catch(Exception ex){

			out.println("Exception: " + ex.getMessage());

		}finally{
			if(!isClosed){
				try{
					conn.close();
				}catch(Exception ex){}
			}
		}
	}

	private String PA_getArticleList(HttpServletRequest request, Connection conn, String spn){

		int pn = PA_strToInt( spn );

		String strQuery = null;
		int nCount = 0;

		StringBuilder sb = new StringBuilder();

		try{


			strQuery = String.format("select count(*) from `%s`", strTableName);
			nCount = (int)getOne(request, conn, strQuery);

			sb.append("<ul class=\"page\">\n");
			for(int i = 0; i < nCount; i += 20){
				sb.append("\t<li><a href=\"?pn=").append(i).append("\">").append(i).append("</a></li>\n");
			}
			sb.append("</ul>\n\n");

			sb.append("<ul>\n");
			strQuery = String.format("select `id`, `strTitle`, `strDate` from `%s` where `flag`=1 limit %d, %d", strTableName, pn, 20);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(strQuery);
			while(rs.next()){
				int id = rs.getInt("id");
				String strDate = rs.getString("strDate");
				sb.append("\t<li>").append(id).append(". <a href=\"?id=").append(id).append("\">");
				sb.append( HtmlEncode( rs.getString("strTitle") ) ).append("</a>");
				if(strDate != null){
					sb.append(" <em>").append(strDate).append("</em>");
				}
				sb.append("</li>\n");
			}
			sb.append("</ul>\n\n");
			rs.close();
			stmt.close();

		}catch(Exception ex){

			sb.append("Exception: " + ex.getMessage());

			ex.printStackTrace();

			//return "Exception: " + ex.getMessage();

		}

		return sb.toString();
	}

	private String PA_getArticleForm(HttpServletRequest request, Connection conn, String sid){
		StringBuilder sb = new StringBuilder();
		boolean showForm = true;

		String id = null;
		String strTitle = null;
		String strDate = null;
		String strFrom = null;
		String strFromLink = null;
		String strContent = null;
		if(sid == null){
			strDate = getNowString();
		} else {
			Map<String, String> map = getArticleDetailsData(request, conn, sid);
			if(map == null){
				showForm = false;
			} else {
				id = map.get("id");
				strTitle = map.get("strTitle");
				strDate = map.get("strDate");
				strFrom = map.get("strFrom");
				strFromLink = map.get("strFromLink");
				strContent = map.get("strContent");
			}
		}

		if(showForm){
			String act = sid == null ? "da" : "dm";
			sb.append("<form method=\"POST\" action=\"?\">\n");
			sb.append("<input type=\"hidden\" name=\"act\" value=\"").append(act).append("\" />\n");
			if(id != null) {
				sb.append("<input type=\"hidden\" name=\"id\" value=\"").append(id).append("\" />\n");
			}
			sb.append("\t<p>\n");
			sb.append("\t\t<input type=\"submit\" />\n");
			sb.append("\t</p>\n");
			sb.append("\t<dl>\n");
			sb.append("\t\t<dt><strong>strTitle</strong></dt>\n");
			sb.append("\t\t<dd><input type=\"text\" name=\"txtTitle\" value=\"").append(HtmlEncode(strTitle)).append("\" size=\"50\" /></dd>\n");
			sb.append("\t</dl>\n");
			sb.append("\t<dl>\n");
			sb.append("\t\t<dt><strong>strDate</strong></dt>\n");
			sb.append("\t\t<dd><input type=\"text\" name=\"txtDate\" value=\"").append(HtmlEncode(strDate)).append("\" /></dd>\n");
			sb.append("\t</dl>\n");
			sb.append("\t<dl>\n");
			sb.append("\t\t<dt><strong>strFrom</strong></dt>\n");
			sb.append("\t\t<dd><input type=\"text\" name=\"txtFrom\" value=\"").append(HtmlEncode(strFrom)).append("\" size=\"25\" /></dd>\n");
			sb.append("\t</dl>\n");
			sb.append("\t<dl>\n");
			sb.append("\t\t<dt><strong>strFromLink</strong></dt>\n");
			sb.append("\t\t<dd><input type=\"text\" name=\"txtFromLink\" value=\"").append(HtmlEncode(strFromLink)).append("\" size=\"50\" /></dd>\n");
			sb.append("\t</dl>\n");
			sb.append("\t<dl>\n");
			sb.append("\t\t<dt><strong>strContent</strong></dt>\n");
			sb.append("\t\t<dd><textarea name=\"txtContent\" cols=\"180\" rows=\"20\">").append(HtmlEncode(strContent)).append("</textarea></dd>\n");
			sb.append("\t</dl>\n");
			sb.append("</form>\n");
		}else{
			sb.append("Nothing...");
		}
		return sb.toString();
	}

	private String PA_getArticleDetails(HttpServletRequest request, Connection conn, String sid){
		StringBuilder sb = new StringBuilder();
		Map<String, String> map = getArticleDetailsData(request, conn, sid);
		if(map != null){
			sb.append("<span class=\"nav\" style=\"float:right\"><a href=\"?act=m&id=").append(map.get("id"));
			sb.append("\" class=\"modify\"></a></span>\n\n\n");
			sb.append("<h1>").append(map.get("strTitle")).append("</h1>\n");
			sb.append("<p><strong>Date:</strong> ").append(map.get("strDate")).append("</p>\n");
			sb.append("<p><strong>From:</strong> ").append(HtmlEncode(map.get("strFrom"))).append("</p>\n");
			sb.append("<p><strong>FromLink:</strong> ").append(HtmlEncode(map.get("strFromLink"))).append("</p>\n");

			sb.append("<pre>").append(HtmlEncode(map.get("strContent"))).append("</pre>\n");

			sb.append("<p><strong>DateCreated:</strong> ").append(map.get("strDateCreated")).append("</p>\n");
			sb.append("<p><strong>DateModified:</strong> ").append(map.get("strDateModified")).append("</p>\n");
		}else{
			sb.append("Nothing...");
		}

		/*
		if (sid == null) return null;
		int id = PA_strToInt(sid);
		try{

			String strQuery = String.format("select `strTitle`, `strDate`, `strFrom`, `strFromLink`, `strContent`, `strDateCreated`, `strDateModified` from `%s` where `id`=%d", strTableName, id);

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(strQuery);
			if( rs.next() ){

				String strTitle = rs.getString("strTitle");
				String strDate = rs.getString("strDate");
				String strFrom = rs.getString("strFrom");
				String strFromLink = rs.getString("strFromLink");
				String strContent = rs.getString("strContent");
				String strDateCreated = rs.getString("strDateCreated");
				String strDateModified = rs.getString("strDateModified");

				sb.append("<span class=\"nav\" style=\"float:right\"><a href=\"?act=m&id=").append(id);
				sb.append("\" class=\"modify\"></a></span>\n\n\n");
				sb.append("<h1>").append(HtmlEncode(strTitle)).append("</h1>\n");
				sb.append("<p><strong>Date:</strong> ").append(strDate).append("</p>\n");
				sb.append("<p><strong>From:</strong> ").append(HtmlEncode(strFrom)).append("</p>\n");
				sb.append("<p><strong>FromLink:</strong> ").append(HtmlEncode(strFromLink)).append("</p>\n");

				sb.append("<pre>").append(HtmlEncode(strContent)).append("</pre>\n");

				sb.append("<p><strong>DateCreated:</strong> ").append(strDateCreated).append("</p>\n");
				sb.append("<p><strong>DateModified:</strong> ").append(strDateModified).append("</p>\n");
			}
			rs.close();

		}catch(Exception ex){
			return "Exception: " + ex.getMessage();
		}
		*/
		return sb.toString();
	}

	private String dbs(String s){
		return s == null || s == "" ? "null" : "'" + s.replace("'", "''") + "'";
	}

	private String PA_doArticleDetailsUpdate(HttpServletRequest request, Connection conn, String act){
		//if(act.equals("da")
		String strMessage = null;
		if(act == null || !(act.equals("da") || act.equals("dm"))){
			strMessage = "act=" + act;
		} else {
			StringBuilder sb = new StringBuilder();

			int id = 0;
			String sid = request.getParameter("id");
			String strTitle = request.getParameter("txtTitle");
			String strDate = request.getParameter("txtDate");
			String strFrom = request.getParameter("txtFrom");
			String strFromLink = request.getParameter("txtFromLink");
			String strContent = request.getParameter("txtContent");

			String strDateNow = getNowString();

			String strQuery = null;
			int nAffectedCount = 0;

			if(act.equals("da")){

				Object oid = getOne(request, conn, "select max(`id`) from `" + strTableName + "`");
				id = oid == null ? 1 : (int)oid + 1;

				sb.append("insert into `").append(strTableName).append("`");
				sb.append("(`id`, `strTitle`, `strDate`, `strFrom`, `strFromLink`, `strContent`, `strDateCreated`, `flag`) values(");
				sb.append(id).append(", ");
				sb.append( dbs(strTitle) ).append(", ");
				sb.append( dbs(strDate) ).append(", ");
				sb.append( dbs(strFrom) ).append(", ");
				sb.append( dbs(strFromLink) ).append(", ");
				sb.append( dbs(strContent) ).append(", ");
				sb.append( "'" ).append( strDateNow ).append("', ");
				sb.append( "1);");

				// strMessage = sb.toString();

				strQuery = sb.toString();

				try{
					Statement stmt = conn.createStatement();
					nAffectedCount = stmt.executeUpdate(strQuery);
					stmt.close();
					strMessage = "act=" + act + ", nAffectedCount=" + nAffectedCount;

				}catch(Exception ex){
					strMessage = "Exception: " + ex.getMessage();
				}

			}else{ // dm

				id = PA_strToInt( sid );

				sb.append("update `").append(strTableName).append("` set ");

				sb.append("`strTitle`=").append( dbs(strTitle) ).append(", ");
				sb.append("`strDate`=").append( dbs(strDate) ).append(", ");
				sb.append("`strFrom`=").append( dbs(strFrom) ).append(", ");
				sb.append("`strFromLink`=").append( dbs(strFromLink) ).append(", ");
				sb.append("`strContent`=").append( dbs(strContent) ).append(", ");
				sb.append("`strDateModified`='").append( strDateNow ).append("' ");
				sb.append("where id=").append(id);

				//strMessage = sb.toString();

				strQuery = sb.toString();

				try{
					Statement stmt = conn.createStatement();
					nAffectedCount = stmt.executeUpdate(strQuery);
					stmt.close();
					strMessage = "act=" + act + ", nAffectedCount=" + nAffectedCount;

				}catch(Exception ex){
					strMessage = "Exception: " + ex.getMessage();
				}

			}
		}
		return strMessage;
	}

	public String HtmlEncode(String s){
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public String getNowString(){
		return dateFormat.format(new Date());
	}

	public int PA_strToInt(String s){
		int n = 0;
		try{
			n = Integer.parseInt(s);
		}catch(Exception ex){}
		return n;
	}

	private Map<String, String> getArticleDetailsData(HttpServletRequest request, Connection conn, String sid){
		Map<String, String> map = null;
		if (sid == null || conn == null) return null;
		int id = PA_strToInt(sid);
		try{
			String strQuery = String.format("select `strTitle`, `strDate`, `strFrom`, `strFromLink`, `strContent`, `strDateCreated`, `strDateModified` from `%s` where `id`=%d", strTableName, id);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(strQuery);
			if(rs.next()){
				map = new HashMap<String, String>();

				map.put("id", String.valueOf(id));
				map.put("strTitle", rs.getString("strTitle"));
				map.put("strDate", rs.getString("strDate"));
				map.put("strFrom", rs.getString("strFrom"));
				map.put("strFromLink", rs.getString("strFromLink"));
				map.put("strContent", rs.getString("strContent"));
				map.put("strDateCreated", rs.getString("strDateCreated"));
				map.put("strDateModified", rs.getString("strDateModified"));
			}
			rs.close();
			stmt.close();
		}catch(Exception ex){}
		return map;
	}

	public Object getOne(HttpServletRequest request, Connection conn, String strQuery){
		Object o = null;
		if(conn != null){
			try{
				//Connection conn = getConnection(request);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(strQuery);
				if(rs.next()){
					o = rs.getObject(1);
				}
				rs.close();
				stmt.close();
			}catch(Exception ex){}
		}
		return o;
	}

	public Connection getConnection(HttpServletRequest request){
		Connection conn = null;
		try{
			Class.forName(strDriver);
			// String strDataFile = request.getRealPath("/WEB-INF/" + dataFile);
			String strDataFile = request.getSession().getServletContext().getRealPath("/WEB-INF/" + dataFile);
			conn = DriverManager.getConnection("jdbc:sqlite:" + strDataFile);

		}catch(Exception ex){}
		return conn;
	}


	/*
	public static void main(String[] args){
		try{
			Class.forName(strDriver);
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dataFile);
			Statement stmt = conn.createStatement();
			String strQuery = "create table if not exists demo1(id int, strName varchar(255));";
			stmt.executeUpdate(strQuery);
			strQuery = "delete from demo1;";
			stmt.executeUpdate(strQuery);
			strQuery = "insert into demo1(id, strName) values(1, '冰红茶');";
			stmt.executeUpdate(strQuery);
			strQuery = "insert into demo1(id, strName) values(2, '王老吉');";
			stmt.executeUpdate(strQuery);
			strQuery = "select * from demo1;";
			ResultSet rs = stmt.executeQuery(strQuery);
			while(rs.next()){
				System.out.println("id=" + rs.getInt(1) + ", strName=" + rs.getString("strName"));
			}
			rs.close();
			conn.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	*/

}
