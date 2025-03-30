// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "../../../../openzeppelin/ERC20.sol";

interface IBlacklist {
    function isBlacklisted(address account) external view returns (bool);
}

contract ISTCoin is ERC20 {
    IBlacklist private blacklist;

    constructor(
        uint256 totalSupply,
        address blacklistAddress
    ) ERC20("ISTCoint", "IST") {
        blacklist = IBlacklist(blacklistAddress);
        _mint(msg.sender, totalSupply * 10 ** decimals());
    }

    function transfer(
        address to,
        uint256 amount
    ) public override returns (bool) {
        require(!blacklist.isBlacklisted(msg.sender), "Sender is blacklisted");
        require(!blacklist.isBlacklisted(to), "Receiver is blacklisted");
        return super.transferFrom(msg.sender, to, amount);
    }

    function transferFrom(
        address from,
        address to,
        uint256 amount
    ) public override returns (bool) {
        require(!blacklist.isBlacklisted(from), "Sender is blacklisted");
        require(!blacklist.isBlacklisted(to), "Receiver is blacklisted");
        return super.transferFrom(from, to, amount);
    }
}
