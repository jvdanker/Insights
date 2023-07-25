package net.vdanker;

import net.vdanker.mappers.GitStreams;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.h2.tools.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.Date;
import java.util.*;

public class DbServer {

    static String URL = "jdbc:h2:./test";

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Server server = Server.createTcpServer("-tcpPort", "9092", "-tcpAllowOthers").start();
        System.out.println("Server started...");
    }
}
