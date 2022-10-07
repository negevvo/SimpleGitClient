import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;

public class GitClient {
    public final String PATH, URL;

    private Git _git;

    private UsernamePasswordCredentialsProvider _credentials;

    enum CloneStatus{ SUCCESS, UNAUTHORIZED, FOLDER_EXISTS, OTHER }
    enum PushStatus{ SUCCESS, INIT_REQUIRED }

    public GitClient(String localPath, String repoUrl) {
        this.PATH = localPath;
        this.URL = repoUrl;
    }

    public void login(String username, String password){
        this._credentials = new UsernamePasswordCredentialsProvider(username, password);
    }

    public void commitAll(String message) throws GitAPIException {
        CommitCommand command = this._git.commit();
        this._git.add().addFilepattern(".").call();
        this._git.add().setUpdate(true).addFilepattern(".").call();
        command.setMessage(message);
        command.call();
    }

    public PushStatus push(String message) throws GitAPIException {
        if(this._git == null) return PushStatus.INIT_REQUIRED;
        this.commitAll(message);
        PushCommand command = this._git.push();
        command.setCredentialsProvider(this._credentials);
        command.setPushAll();
        command.call();
        return PushStatus.SUCCESS;
    }

    public PushStatus push() throws GitAPIException {
        return this.push(Strings.DEFAULT_COMMIT_MESSAGE);
    }

    public CloneStatus cloneRepo() {
        CloneCommand command = Git.cloneRepository();
        command.setURI(URL);
        command.setDirectory(new File(PATH));
        command.setCredentialsProvider(this._credentials);
        try{
            this._git = command.call();
        }catch(TransportException e){
            return CloneStatus.UNAUTHORIZED;
        }catch(JGitInternalException e){
            return CloneStatus.FOLDER_EXISTS;
        }catch(Exception e){
            return CloneStatus.OTHER;
        }
        return CloneStatus.SUCCESS;
    }
}
