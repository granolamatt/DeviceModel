/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package devicemodel;

/**
 *
 * @author root
 */
public abstract class NodeHandler {    
    // return a true/false if the node should also follow its
    // normal update routine as well; unused by set
    public abstract boolean handle(DeviceNode node);
}
