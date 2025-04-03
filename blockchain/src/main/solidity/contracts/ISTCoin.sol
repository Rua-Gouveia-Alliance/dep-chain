// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "../../../../openzeppelin/ERC20.sol";
import "./Blacklist.sol";


contract ISTCoin is ERC20, Blacklist {

    constructor(
        uint256 totalSupply
    ) ERC20("ISTCoin", "IST") Blacklist() {
        _mint(msg.sender, totalSupply * 10 ** decimals());
    }

    function transfer(
        address to,
        uint256 amount
    ) public override returns (bool) {
        require(!isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!isBlacklisted(to), "Receiver is blacklisted");
        return super.transfer(to, amount);
    }

    function transferFrom(
        address from,
        address to,
        uint256 amount
    ) public override returns (bool) {
        require(!isBlacklisted(from), "Sender is blacklisted");
        require(!isBlacklisted(to), "Receiver is blacklisted");
        return super.transferFrom(from, to, amount);
    }
}
