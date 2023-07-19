import React, {useState} from 'react';
import * as d3 from "d3";
import CommitsView from "./CommitsView";
import {useOnce} from "./UseOnce";
import FocusViewBars from "./FocusViewBars";
import {Histogram} from "./Histogram";

const config_size = {
    width: 800,
    height: 200,
    margin: {
        left: 30,
        right: 20,
        top: 20,
        bottom: 20,
    },
}

const focus_size = {
    width: 800,
    height: 50,
    margin: {
        left: 30,
        right: 20,
        top: 0,
        bottom: 20,
    },
}

function BarChart() {
    const [data, setData] = useState([]);
    const [chartData, setChartData] = useState([]);
    const [updateChart, setUpdateChart] = useState({});
    const [committersPerDay, setCommittersPerDay] = useState([]);

    function convertData(data) {
        return data
            .map(d => {
                return {
                    epoch: new Date((+d.epoch) * 86400000),
                    count: +d.count, // Math.random(), //+d.count,
                    committers: +d.committers,
                    countNormalized: +d.count / +d.committers,
                    files: +d.files
                }
            })
        ;
    }

    function groupByYear() {
        // console.log(data);

        // data.map(d => {
        //     // console.log(d, d3.timeYear(d.epoch));
        // });

        const x = data.reduce((acc, curr) => {
            const key = d3.timeYear(curr.epoch).valueOf();
            // console.log(key, acc.get(key));
            return acc.set(key, (acc.get(key) || 0) + curr.count);
        }, new Map());

        const group = Array.from(
            x,
            (i) => { return {epoch: i[0], count: i[1]} });
        console.table(group);
        setChartData(group);
    }

    function updateCallback(focusedArea) {
        console.log('updateCallback', focusedArea);

        const commits = data.filter(d => focusedArea[0] < d.epoch && d.epoch < focusedArea[1]);
        console.table(commits);

        const committersPerDayFiltered = committersPerDay.filter(d => focusedArea[0] < d.epoch && d.epoch < focusedArea[1]);
        console.table(committersPerDayFiltered);

        const daysInSelection = (focusedArea[1] - focusedArea[0]) / 86400000;
        const totalCommits = commits.reduce((acc, curr) => acc + curr.count, 0);
        const mean = d3.mean(commits, c=> c.count); // totalCommits / daysInSelection;
        const commitsDev = d3.deviation(commits, c => c.count);
        const totalFilesTouched = commits.reduce((acc, curr) => acc + curr.files, 0); // FIXME dezelfde files?
        const mergedCommitters = committersPerDayFiltered.reduce((acc, curr) => d3.union(acc, curr.committers), new Set());
        console.table(mergedCommitters);

        console.log(`Selection from ${focusedArea[0]?.toLocaleString()} to ${focusedArea[1]?.toLocaleString()}`);
        console.log(`Selection from ${focusedArea[0]} to ${focusedArea[1]}`);
        console.log(`Days in selection ${daysInSelection}`);
        console.log(`Number of commits ${totalCommits}`);
        console.log(`Number of committers ${mergedCommitters.size}`);
        console.log(`Commits per day (mean) ${mean}`); // x_
        console.log(`Commits per day (SD) ${commitsDev}`);  // mu
        console.log(`Total number of files touched ${totalFilesTouched}`);

        // correlation
        // regression
        // r^2

        // commits vs size of file
        // commits vs number of methods
        // commits touching same section of a file
        // duplicate files across repos
        // dead vs alive files

        // dev vs dev commits

        setUpdateChart(focusedArea);
    }

    function mockData() {
        return [{
            "epoch": new Date("2022-02-01T00:00:00.000Z"),
            "count": 1,
            "committers": 1,
            "countNormalized": 1,
            "files": 3
        },{
            "epoch": new Date("2023-02-01T00:00:00.000Z"),
            "count": 2,
            "committers": 1,
            "countNormalized": 1,
            "files": 9
        },{
            "epoch": new Date("2024-02-01T00:00:00.000Z"),
            "count": 1,
            "committers": 1,
            "countNormalized": 1,
            "files": 3
        }];
    }

    function restrictData() {
        return data
            .splice(0, 1000)
        ;
        // .splice(1, 2)
    }

    function decode(s) {
        let x = decodeURIComponent(s);
        return x.replaceAll('%5B', '[')
            .replaceAll('%5D', ']')
            .replaceAll('%2C', ',')
            .replaceAll('+', ' ')
            ;
    }

    useOnce( () => {
        d3.csv("committers-per-day.csv").then(data => {
            const converted = data.map(d => {
                return {
                    epoch: new Date((+d.epoch) * 86400000),
                    committers: new Set(JSON.parse(decode(d.committers)))
                }});

            setCommittersPerDay(converted);
        });

        d3.csv("commits-per-day.csv").then(data => {
            let convertedData = convertData(data);
            // convertedData = mockData();
            // convertedData = restrictData();
            // console.log(convertedData);

            setData(convertedData);
            setChartData(convertedData);
        });
    });

    return (
        <>
            <CommitsView
                data={chartData}
                config={config_size}
                focusedArea={updateChart}
            />

            <FocusViewBars
                data={data}
                config={focus_size}
                update={updateCallback}
            />

            <Histogram
                data={data}
                focusedArea={updateChart}
            />
        </>
    );
}

export default BarChart;