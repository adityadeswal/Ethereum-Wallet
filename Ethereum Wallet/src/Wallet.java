import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.web3j.codegen.Console.exitError;

public class Wallet {

    public static void main(String args[]) throws IOException, ExecutionException, InterruptedException {


        Web3j web3j = Web3j.build(new InfuraHttpService("https://ropsten.infura.io/v3/6bd6ce2e919644128f2d05ebbf34c09d"));
        try {
            Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().sendAsync().get();
            if (web3ClientVersion.hasError()) {
                exitError("Unable to process response from client: "
                        + web3ClientVersion.getError());
            } else {
                System.out.printf("Connected successfully to client: %s%n",
                        web3ClientVersion.getWeb3ClientVersion());
            }
        } catch (InterruptedException | ExecutionException e) {
            exitError("Problem encountered verifying client: " + e.getMessage());
        }

        Scanner scanner = new Scanner(System.in);
        int input1;
        do {
            System.out.println("-------------------------------------------------------------------");
            System.out.println("Ether Wallet");
            System.out.println("1. Check Balance");
            System.out.println("2. Send Funds");
            System.out.println("3. Latest Block");
            System.out.println("4. Exit");
            System.out.println("-------------------------------------------------------------------");
            input1 = scanner.nextInt();

            if (input1 == 1 || input1 == 2) {
                System.out.println("Enter your wallet address");
                System.out.println("*Don't have wallet create one - press \"x\"*");
                String input2 = scanner.next();
                if (input2.equals("x")) {
                    walletCreator();
                } else {

                    File walletFile = new File(input2);
                    Credentials credentials = getCredentials(walletFile);

                    if (input1 == 1) {
                        EthGetBalance ethGetBalance = web3j
                                .ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                                .sendAsync().get();

                        BigInteger wei = ethGetBalance.getBalance();
                        System.out.println("Your account balance is " + wei + " wei");
                    } else {
                        sendMoney(credentials, web3j);
                    }
                }

            } else if (input1 == 3) {

                EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
                System.out.println(block.getNumber().toString());//Latest
            } else if (input1 != 4) {
                System.out.println("Please enter valid value 1,2,3 or 4");
            }
        } while (input1 != 4);

        System.exit(0);

    }

    private static String getPassword() {
        while (true) {

            Scanner scanner = new Scanner(System.in);

            String input1 = scanner.next();

            System.out.println("Please re-enter your password");

            String input2 = scanner.next();

            if ((input1.equals(input2))) {
                return input1;
            } else {
                System.out.println("Sorry, passwords did not match\n");
            }
        }
    }

    private static String getDestinationDir() {
        Scanner scanner = new Scanner(System.in);
        String defaultDir = WalletUtils.getTestnetKeyDirectory();
        System.out.println("Please enter a destination directory location [" + defaultDir + "]: ");
        String destinationDir = scanner.next();
        if (destinationDir.equals("")) {
            return defaultDir;
        } else {
            return destinationDir;
        }
    }

    private static File createDir(String destinationDir) {
        File destination = new File(destinationDir);

        if (!destination.exists()) {
            System.out.println("Creating directory: " + destinationDir + " ...");
            if (!destination.mkdirs()) {
                exitError("Unable to create destination directory ["
                        + destinationDir + "], exiting...");
            } else {
                System.out.println("complete\n");
            }
        }

        return destination;
    }

    private static Credentials getCredentials(File walletFile) {
        if (!walletFile.exists() || !walletFile.isFile()) {
            exitError("Unable to read wallet file: " + walletFile);
        }
        return loadWalletFile(walletFile);
    }

    static private Credentials loadWalletFile(File walletFile) {
        while (true) {
            Scanner scanner = new Scanner(System.in);

            System.out.println("Please enter your existing wallet file password: ");

            String currentPassword = scanner.next();
            try {
                return WalletUtils.loadCredentials(currentPassword, walletFile);
            } catch (CipherException e) {
                System.out.print("Invalid password specified\n");
            } catch (IOException e) {
                exitError("Unable to load wallet file: " + walletFile + "\n" + e.getMessage());
            }
        }
    }

    private static void walletCreator() {

        System.out.println("Please enter a wallet password");
        String password = Wallet.getPassword();
        String destinationDir = Wallet.getDestinationDir();
        File destination = Wallet.createDir(destinationDir);

        try {
            String walletFileName = WalletUtils.generateFullNewWalletFile(password, destination);
            System.out.println("Wallet file " + walletFileName
                    + " successfully created in: " + destinationDir + "\n");
        } catch (CipherException | IOException | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();

        }
    }

    private static void sendMoney(Credentials credentials, Web3j web3j) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter destination Address");
        String destinationAddress = scanner.next();
        if (!WalletUtils.isValidAddress(destinationAddress)) {
            exitError("Invalid destination address specified");
        }
        System.out.println("Enter amount in wei");
        BigDecimal amount = scanner.nextBigDecimal();
        try {
            Future<TransactionReceipt> future = Transfer.sendFunds(
                    web3j, credentials, destinationAddress, amount, Convert.Unit.WEI)
                    .sendAsync();


            while (!future.isDone()) {
                System.out.print(".");
                Thread.sleep(500);
            }
            System.out.printf("$%n%n");
            System.out.println("Amount transferred successfully");
            System.out.println(credentials.getAddress() + " to " + destinationAddress);
            EthGetBalance ethGetBalance = web3j
                    .ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                    .sendAsync().get();

            BigInteger wei = ethGetBalance.getBalance();
            System.out.println("Current Balance: " + wei);
            System.out.println("Transaction Hash: " + future.get().getTransactionHash());
            System.out.println(future.get());
        } catch (InterruptedException | ExecutionException | TransactionException | IOException e) {
            exitError("Problem encountered transferring funds: \n" + e.getMessage());
        }


    }


}
