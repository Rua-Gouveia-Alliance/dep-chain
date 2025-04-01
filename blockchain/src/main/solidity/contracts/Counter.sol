// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract Counter {
    uint256 cnt;

    constructor () {
        cnt = 2;
    }

    function increment() public {
        cnt++;
    }

    function addNum(uint256 num) public {
        cnt += num;
    }

    function displayCount() public view returns (uint256){
        return cnt;
    }
}
