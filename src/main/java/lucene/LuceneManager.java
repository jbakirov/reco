package lucene;

import dbmanager.DbManager;
import models.Item;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import utils.JsonHandler;

import java.awt.print.Book;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Baka on 02.09.2015.
 */
public class LuceneManager {

    public static final File BOOK_INDEX_DIRECTORY = new File("BooksIndexDirectory");
    public static final File MUSIC_INDEX_DIRECTORY = new File("MusicIndexDirectory");

    public static void main (String[] args){
        new LuceneManager().createIndexForMusic();
//        System.out.println(new LuceneManager().searchMusic("numb linkin park"));

    }

    public void createIndexForBooks() {
        System.out.println("Indexing Books");
        IndexWriter writer = null;
        StandardAnalyzer analyzer = null;
        ResultSet rs = null;
        Statement stmt = null;
        Connection conn = null;

        try {
            DbManager dbManager = new DbManager();
            conn = dbManager.getConnectionPool().getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT id, name, author, poster_url, year FROM books";
            rs = stmt.executeQuery(sql);

            analyzer = new StandardAnalyzer(Version.LUCENE_36);
            writer = new IndexWriter(
                    FSDirectory.open(BOOK_INDEX_DIRECTORY),
                    analyzer,
                    true,
                    IndexWriter.MaxFieldLength.LIMITED);

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String author = rs.getString("author");
                String poster_url = rs.getString("poster_url");
                String year = rs.getString("year");
                String fulltext = name + " " + author + " " + year;

                Document document = new Document();
                Field idField = new Field("bookid", id, Field.Store.YES,
                        Field.Index.NO);
                Field authorField = new Field("author", author, Field.Store.YES,
                        Field.Index.ANALYZED);
                Field nameField = new Field("name", name, Field.Store.YES,
                        Field.Index.ANALYZED);
                Field posterurlField = new Field("poster_url", poster_url, Field.Store.YES,
                        Field.Index.NO);
                Field yearField = new Field("year", year, Field.Store.YES,
                        Field.Index.NOT_ANALYZED);
                Field fulltextField = new Field("fulltext", fulltext, Field.Store.NO,
                        Field.Index.ANALYZED);
                document.add(idField);
                document.add(authorField);
                document.add(nameField);
                document.add(yearField);
                document.add(posterurlField);
                document.add(fulltextField);

                writer.addDocument(document);
            }
            System.out.println("Optimizing index");
            writer.optimize();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void createIndexForMusic(){
        System.out.println("Indexing Books");
        IndexWriter writer = null;
        StandardAnalyzer analyzer = null;
        ResultSet rs = null;
        Statement stmt = null;
        Connection conn = null;

        try {
            DbManager dbManager = new DbManager();
            conn = dbManager.getConnectionPool().getConnection();
            stmt = conn.createStatement();
            String sql = "SELECT id, composition, singer, album_img, genres FROM music";
            rs = stmt.executeQuery(sql);

            analyzer = new StandardAnalyzer(Version.LUCENE_36);
            writer = new IndexWriter(
                    FSDirectory.open(MUSIC_INDEX_DIRECTORY),
                    analyzer,
                    true,
                    IndexWriter.MaxFieldLength.LIMITED);

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("composition");
                String singer = rs.getString("singer");
                String poster_url = rs.getString("album_img");
                String genres = rs.getString("genres");
                String fulltext = name + " " + singer;

                Document document = new Document();
                Field idField = new Field("musicid", id, Field.Store.YES,
                        Field.Index.NO);
                Field singerField = new Field("singer", singer, Field.Store.YES,
                        Field.Index.ANALYZED);
                Field nameField = new Field("name", name, Field.Store.YES,
                        Field.Index.ANALYZED);
                Field posterurlField = new Field("poster_url", poster_url, Field.Store.YES,
                        Field.Index.NO);
                Field genresField = new Field("genres", genres, Field.Store.YES,
                        Field.Index.NOT_ANALYZED);
                Field fulltextField = new Field("fulltext", fulltext, Field.Store.YES,
                        Field.Index.ANALYZED);
                document.add(idField);
                document.add(singerField);
                document.add(nameField);
                document.add(genresField);
                document.add(posterurlField);
                document.add(fulltextField);

                writer.addDocument(document);
            }
            System.out.println("Optimizing index");
            writer.optimize();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
                if (rs != null)
                    rs.close();
                if (stmt != null)
                    stmt.close();
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String searchMusicFullText (String fullText){
        List<Item> items = new ArrayList<>();
        try {
            //Searching
            IndexReader reader = IndexReader.open(FSDirectory.open(MUSIC_INDEX_DIRECTORY));
            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);

            //fullText query
            QueryParser fullTextQP = new QueryParser(Version.LUCENE_36, "fulltext", analyzer);
            Query fullTextQuery = fullTextQP.parse(fullText);


            //final query
            BooleanQuery finalQuery = new BooleanQuery();
            finalQuery.add(fullTextQuery, BooleanClause.Occur.SHOULD);

            TopDocs hits = searcher.search(finalQuery, 10); // run the query

            if (hits.scoreDocs.length <= 0){
                return "No records found";
            }

            for (int i = 0; i < 10; i++) {
                Document doc = searcher.doc(hits.scoreDocs[i].doc);//get the next  document
                Item item = new Item();

                item.setId(Integer.parseInt(doc.get("musicid")));
                item.setName(doc.get("name"));
                item.setSinger(doc.get("singer"));
                item.setGenres(doc.get("genres"));
                item.setPoster_url(doc.get("poster_url"));
                items.add(item);
            }
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e){
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return JsonHandler.toJson(items);
    }

    public String searchMusic(String keyword){
        IndexReader reader = null;
        StandardAnalyzer analyzer = null;
        IndexSearcher searcher = null;
        TopScoreDocCollector collector = null;
        QueryParser parser = null;
        Query query = null;
        ScoreDoc[] hits = null;

        List<Item> items = new ArrayList<>();

        try {
            //create standard analyzer object
            analyzer = new StandardAnalyzer(Version.LUCENE_36);
            //create File object of our index directory
            //create index reader object
            reader = IndexReader.open(FSDirectory.open(MUSIC_INDEX_DIRECTORY), true);
            //create index searcher object
            searcher = new IndexSearcher(reader);
            //create topscore document collector
            collector = TopScoreDocCollector.create(10, false);
            //create query parser object
            parser = new QueryParser(Version.LUCENE_36, "name", analyzer);
            //parse the query and get reference to Query object
            query = parser.parse(keyword);
            //search the query
            searcher.search(query, collector);
            hits = collector.topDocs().scoreDocs;
            //check whether the search returns any result

            if (hits.length > 0) {
                //print heading
                for (int i = 0; i < hits.length; i++) {
                    int scoreId = hits[i].doc;
                    //get reference to document
                    Document document = searcher.doc(scoreId);
                    Item item = new Item();
                    item.setId(Integer.parseInt(document.getField("musicid").stringValue()));
                    item.setName(document.getField("name").stringValue());
                    item.setSinger(document.getField("singer").stringValue());
                    item.setGenres(document.getField("genres").stringValue());
                    item.setPoster_url(document.getField("poster_url").stringValue());
                    items.add(item);
//                    System.out.println("Id: " + document.getField("bookid").stringValue());
//                    System.out.println("Name: " + document.getField("name").stringValue());
//                    System.out.println("Author: " + document.getField("author").stringValue());
//                    System.out.println("Year: " + document.getField("year").stringValue());
//                    System.out.println("Poster Url: " + document.getField("poster_url").stringValue());
//                    System.out.println("------------");
                }
            } else {
                return "No records found";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JsonHandler.toJson(items);

    }

    public String searchBook(String keyword) {
        IndexReader reader = null;
        StandardAnalyzer analyzer = null;
        IndexSearcher searcher = null;
        TopScoreDocCollector collector = null;
        QueryParser parser = null;
        Query query = null;
        ScoreDoc[] hits = null;

        List<Item> books = new ArrayList<>();

        try {
            //create standard analyzer object
            analyzer = new StandardAnalyzer(Version.LUCENE_36);
            //create File object of our index directory
            //create index reader object
            reader = IndexReader.open(FSDirectory.open(BOOK_INDEX_DIRECTORY), true);
            //create index searcher object
            searcher = new IndexSearcher(reader);
            //create topscore document collector
            collector = TopScoreDocCollector.create(10, false);
            //create query parser object
            parser = new QueryParser(Version.LUCENE_36, "name", analyzer);
            //parse the query and get reference to Query object
            query = parser.parse(keyword);
            //search the query
            searcher.search(query, collector);
            hits = collector.topDocs().scoreDocs;
            //check whether the search returns any result
            if (hits.length > 0) {
                //print heading
                for (int i = 0; i < hits.length; i++) {
                    int scoreId = hits[i].doc;
                    //get reference to document
                    Document document = searcher.doc(scoreId);
                    Item item = new Item();
                    item.setId(Integer.parseInt(document.getField("bookid").stringValue()));
                    item.setName(document.getField("name").stringValue());
                    item.setAuthor(document.getField("author").stringValue());
                    item.setYear(document.getField("year").stringValue());
                    item.setPoster_url(document.getField("poster_url").stringValue());
                    books.add(item);
//                    System.out.println("Id: " + document.getField("bookid").stringValue());
//                    System.out.println("Name: " + document.getField("name").stringValue());
//                    System.out.println("Author: " + document.getField("author").stringValue());
//                    System.out.println("Year: " + document.getField("year").stringValue());
//                    System.out.println("Poster Url: " + document.getField("poster_url").stringValue());
//                    System.out.println("------------");
                }
            } else {
                System.out.println("No records found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return JsonHandler.toJson(books);
    }

}
