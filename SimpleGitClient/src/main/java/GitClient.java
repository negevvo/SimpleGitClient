import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

public class GitClient {
    public final String PATH, URL;

    private Git _git;

    private UsernamePasswordCredentialsProvider _credentials;

    enum CloneStatus{ SUCCESS, UNAUTHORIZED, FOLDER_EXISTS, OTHER }
    enum PushStatus{ SUCCESS, INIT_REQUIRED }
    enum MakeStatus{ SUCCESS, FOLDER_DOESNT_EXIST, NOT_A_REPO }
    enum FetchStatus{ SUCCESS, INIT_REQUIRED, UNAUTHORIZED, OTHER }
    enum PullStatus{ SUCCESS, INIT_REQUIRED, UNAUTHORIZED, OTHER }

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

    public PushStatus push(String message, ProgressMonitor monitor) throws GitAPIException {
        if(this._git == null) return PushStatus.INIT_REQUIRED;
        this.commitAll(message);
        PushCommand command = this._git.push();
        command.setCredentialsProvider(this._credentials);
        command.setPushAll();
        if(monitor != null) command.setProgressMonitor(monitor);
        command.call();
        return PushStatus.SUCCESS;
    }

    public PushStatus push(String message) throws GitAPIException {
        return this.push(message, null);
    }

    public PushStatus push() throws GitAPIException {
        return this.push(Strings.DEFAULT_COMMIT_MESSAGE);
    }

    public CloneStatus cloneRepo(ProgressMonitor monitor) {
        CloneCommand command = Git.cloneRepository();
        command.setURI(URL);
        command.setDirectory(new File(PATH));
        command.setCredentialsProvider(this._credentials);
        if(monitor != null) command.setProgressMonitor(monitor);
        try{
            this._git = command.call();
            this._git.getRepository().close();
        }catch(TransportException e){
            return CloneStatus.UNAUTHORIZED;
        }catch(JGitInternalException e){
            return CloneStatus.FOLDER_EXISTS;
        }catch(Exception e){
            return CloneStatus.OTHER;
        }
        return CloneStatus.SUCCESS;
    }

    public CloneStatus cloneRepo() {
        return this.cloneRepo(null);
    }

    public MakeStatus make(){
        File repo = new File(PATH);
        if(!repo.exists()) return MakeStatus.FOLDER_DOESNT_EXIST;
        try {
            this._git = Git.open(repo);
        }catch(RepositoryNotFoundException e){
            return MakeStatus.NOT_A_REPO;
        }catch(IOException e){
            return MakeStatus.FOLDER_DOESNT_EXIST;
        }
        return MakeStatus.SUCCESS;
    }

    public FetchStatus fetch(ProgressMonitor monitor){
        if(this._git == null) return FetchStatus.INIT_REQUIRED;
        FetchCommand command = this._git.fetch();
        command.setCredentialsProvider(this._credentials);
        command.setRemote("origin");
        if(monitor != null) command.setProgressMonitor(monitor);
        try {
            command.call();
        }catch(TransportException | InvalidRemoteException e){
            return FetchStatus.UNAUTHORIZED;
        } catch (GitAPIException e) {
            return FetchStatus.OTHER;
        }
        return FetchStatus.SUCCESS;
    }

    public FetchStatus fetch(){
        return this.fetch(null);
    }

    public PullStatus pull(ProgressMonitor monitor){
        if(this._git == null) return PullStatus.INIT_REQUIRED;
        PullCommand command = this._git.pull();
        command.setCredentialsProvider(this._credentials);
        if(monitor != null) command.setProgressMonitor(monitor);
        try {
            command.call();
        }catch(TransportException | InvalidRemoteException e){
            return PullStatus.UNAUTHORIZED;
        } catch (GitAPIException e) {
            return PullStatus.OTHER;
        }
        return PullStatus.SUCCESS;
    }

    public PullStatus pull(){
        return this.pull(null);
    }
}
