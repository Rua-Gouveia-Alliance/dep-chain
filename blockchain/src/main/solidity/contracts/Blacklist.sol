// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract Blacklist {
    mapping(address => bool) private blacklisted;
    mapping(address => bool) private admins;  // Role-based access control
    address public owner;

    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);
    event AdminAdded(address indexed admin);
    event AdminRemoved(address indexed admin);

    modifier onlyOwner() {
        require(msg.sender == owner, "Not the owner");
        _;
    }

    modifier onlyAdmin() {
        require(admins[msg.sender], "Not an admin");
        _;
    }

    constructor() {
        owner = msg.sender;
        admins[msg.sender] = true;  // Owner is an admin by default
    }

    function addAdmin(address account) public onlyOwner {
        admins[account] = true;
        emit AdminAdded(account);
    }

    function removeAdmin(address account) public onlyOwner {
        admins[account] = false;
        emit AdminRemoved(account);
    }

    function addToBlacklist(address account) public onlyAdmin {
        blacklisted[account] = true;
        emit AddedToBlacklist(account);
    }

    function removeFromBlacklist(address account) public onlyAdmin {
        blacklisted[account] = false;
        emit RemovedFromBlacklist(account);
    }

    function isBlacklisted(address account) public view returns (bool) {
        return blacklisted[account];
    }

    function isAdmin(address account) public view returns (bool) {
        return admins[account];
    }
}
