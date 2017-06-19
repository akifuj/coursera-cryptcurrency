// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {

    private class BlockNode {
      public Block b;
      public BlockNode parent;
      public int height;
      private UTXOPool uPool;


      public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
        this.b = b;
        this.parent = parent;
        this.uPool = uPool;
        if (parent != null) {
          height = parent.height + 1;
        } else {
          height = 1;
        }
      }

      public UTXOPool getUTXOPoolCopy() {
        return new UTXOPool(uPool);
      }
    }

    public static final int CUT_OFF_AGE = 10;

    private HashMap<ByteArrayWrapper, BlockNode> blockChain;
    private BlockNode maxHeightNode;
    private TransactionPool txPool;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        blockChain = new HashMap<>();
        UTXOPool utxoPool = new UTXOPool();
        addCoindbaseToUTXOPool(genesisBlock, utxoPool);
        BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool);
        blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
        maxHeightNode = genesisNode;
        txPool = new TransactionPool();
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return maxHeightNode.b;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightNode.getUTXOPoolCopy();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        byte[] prevBlockHash = block.getPrevBlockHash();
        if (prevBlockHash == null) {
          return false;
        }
        BlockNode parentBlockNode = blockChain.get(new ByteArrayWrapper(prevBlockHash));
        if (parentBlockNode == null) {
          return false;
        }
        TxHandler handler = new TxHandler(parentBlockNode.getUTXOPoolCopy());
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = handler.handleTxs(txs);
        if (validTxs.length != txs.length) {
          return false;
        }
        int proposedHeight = parentBlockNode.height + 1;
        if (proposedHeight <= maxHeightNode.height - CUT_OFF_AGE) {
          return false;
        }
        UTXOPool utxoPool = handler.getUTXOPool();
        addCoindbaseToUTXOPool(block, utxoPool);
        BlockNode node = new BlockNode(block, parentBlockNode, utxoPool);
        blockChain.put(new ByteArrayWrapper(block.getHash()), node);
        if (proposedHeight > maxHeightNode.height) {
          maxHeightNode = node;
        }
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    private void addCoindbaseToUTXOPool(Block block, UTXOPool utxoPool) {
      Transaction coinvase = block.getCoinbase();
      for (int i = 0; i< coinvase.numOutputs(); i++) {
        Transaction.Output out = coinvase.getOutput(i);
        UTXO utxo = new UTXO(coinvase.getHash(), i);
        utxoPool.addUTXO(utxo, out);
      }
    }
}
