window.addEventListener('load', run);

const countryData = {
    items: ['a', 'b', 'c'],
    addItem(item) {
        this.items.push(item);
    },
    removeItem(index) {
        this.items.splice(index, 1);
    },
    updateItem(index, newItem) {
        this.items[index] = newItem;
    }
};

function select(selection, dataFn, idFn) {
    return selection
        .selectAll('li')
        .data(dataFn(), idFn);
}

const render = (element, props) => {
    const selection = select(
        element,
        () => props.countryData.items,
        (d) => d);

    selection
        .enter()
            .append('li')
                .text(d => d);

    selection
        .exit()
            .remove();
};

function run() {
    let element = d3.select('ul');

    render(element, {countryData});

    setTimeout(() => {
        countryData.addItem('test');
        render(element, {countryData});
    }, 1000);

    setTimeout(() => {
        countryData.removeItem(0);
        render(element, {countryData});
    }, 2000);

    setTimeout(() => {
        countryData.updateItem(1, 'test2');
        render(element, {countryData});
    }, 3000);
}